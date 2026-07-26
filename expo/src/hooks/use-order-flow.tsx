import AsyncStorage from '@react-native-async-storage/async-storage';
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

import { DATA_SOURCE } from '@/core/config';
import { getAccessToken } from '@/core/session-store';
import { setForceMock } from '@/core/runtime-data-source';
import { DEFAULT_SPACE } from '@/domain/checkout-fixtures';
import { getCatalog } from '@/data/catalog-repository';
import {
  createStudentCheckout,
  generateIdempotencyKey,
  listOrders,
} from '@/data/order-repository';
import { cartPreview, parseMoney, productUnitPreview, validateSelections } from '@/domain/contract-rules';
import { cartLineKey } from '@/domain/models';
import type {
  CartLine,
  Catalog,
  CreateOrderRequest,
  OrderDetail,
  OrderDestination,
  PaymentMethod,
  Product,
} from '@/domain/models';
import { useWallet } from '@/hooks/use-wallet';

const TEST_ONLY_KEY = 'vaiinilla.test_only_mode';

interface OrderFlowState {
  loading: boolean;
  errorMessage: string | null;
  catalog: Catalog | null;
  searchQuery: string;
  selectedCategoryId: number | null;
  cartLines: CartLine[];
  selectedProductId: number | null;
  selectedOptionIds: number[];
  selectedQuantity: number;
  kitchenNotes: string;
  checkoutDestination: OrderDestination;
  checkoutPayment: PaymentMethod;
  selectedSpaceId: number;
  submitting: boolean;
  submitError: string | null;
  createdOrder: OrderDetail | null;
  clientOrders: OrderDetail[];
  selectedOrderId: string | null;
  testOnlyMode: boolean;
}

interface OrderFlowContextValue extends OrderFlowState {
  cartCount: number;
  cartTotal: string;
  filteredProducts: Product[];
  selectedProduct: Product | null;
  isSelectedProductValid: boolean;
  selectedProductPreviewPrice: string;
  selectedProductPreviewTotal: string;
  canCreateOrder: boolean;
  selectedOrder: OrderDetail | null;
  loadCatalog: () => Promise<void>;
  refreshClientOrders: () => Promise<void>;
  setSearchQuery: (value: string) => void;
  setSelectedCategoryId: (value: number | null) => void;
  openProduct: (productId: number) => void;
  closeProduct: () => void;
  toggleOption: (groupId: number, optionId: number) => void;
  setSelectedQuantity: (quantity: number) => void;
  addSelectedProductToCart: () => void;
  updateCartQuantity: (lineKey: string, delta: number) => void;
  clearCart: () => void;
  setCartLines: (lines: CartLine[]) => void;
  setKitchenNotes: (notes: string) => void;
  setCheckoutDestination: (destination: OrderDestination) => void;
  setCheckoutPayment: (payment: PaymentMethod) => void;
  setSelectedSpaceId: (spaceId: number) => void;
  setCreatedOrder: (order: OrderDetail | null) => void;
  setClientOrders: (orders: OrderDetail[], selectedId: string | null) => void;
  selectOrder: (orderId: string | null) => void;
  confirmOrder: () => Promise<OrderDetail | null>;
  clearCreatedOrder: () => void;
  setTestOnlyMode: (enabled: boolean) => void;
}

const defaultState: OrderFlowState = {
  loading: true,
  errorMessage: null,
  catalog: null,
  searchQuery: '',
  selectedCategoryId: null,
  cartLines: [],
  selectedProductId: null,
  selectedOptionIds: [],
  selectedQuantity: 1,
  kitchenNotes: '',
  checkoutDestination: 'para_llevar',
  checkoutPayment: 'efectivo',
  selectedSpaceId: DEFAULT_SPACE.id,
  submitting: false,
  submitError: null,
  createdOrder: null,
  clientOrders: [],
  selectedOrderId: null,
  testOnlyMode: true,
};

const OrderFlowContext = createContext<OrderFlowContextValue | null>(null);

function getDefaultSelections(product: Product): number[] {
  return product.optionGroups.flatMap((group) => {
    if (group.minimumSelections === 0) {
      return [];
    }
    return group.options.slice(0, group.minimumSelections).map((option) => option.id);
  });
}

export function OrderFlowProvider({ children }: { children: ReactNode }) {
  const wallet = useWallet();
  const [state, setState] = useState<OrderFlowState>(defaultState);

  const loadCatalog = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, errorMessage: null }));
    try {
      const catalog = await getCatalog();
      setState((current) => ({ ...current, loading: false, catalog }));
    } catch (error) {
      setState((current) => ({
        ...current,
        loading: false,
        errorMessage: error instanceof Error ? error.message : 'No pudimos cargar el catálogo.',
      }));
    }
  }, []);

  const refreshClientOrders = useCallback(async () => {
    try {
      const orders = await listOrders('cliente');
      setState((current) => ({
        ...current,
        clientOrders: orders,
        selectedOrderId: current.selectedOrderId ?? orders[0]?.summary.id ?? null,
      }));
    } catch {
      // Keep local gallery seeds if list fails.
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      const stored = await AsyncStorage.getItem(TEST_ONLY_KEY);
      const testOnly = stored === null ? true : stored === 'true';
      setForceMock(testOnly);
      if (!cancelled) {
        setState((current) => ({ ...current, testOnlyMode: testOnly }));
      }

      // REMOTE without session: wait for role login — don't flash "Falta el token".
      if (DATA_SOURCE === 'REMOTE' && !testOnly) {
        const token = await getAccessToken();
        if (!token) {
          if (!cancelled) {
            setState((current) => ({
              ...current,
              loading: false,
              errorMessage: null,
              catalog: null,
            }));
          }
          return;
        }
      }

      if (!cancelled) {
        await loadCatalog();
        await refreshClientOrders();
      }
    };

    void bootstrap();
    return () => {
      cancelled = true;
    };
  }, [loadCatalog, refreshClientOrders]);

  const setTestOnlyMode = useCallback(
    (enabled: boolean) => {
      void AsyncStorage.setItem(TEST_ONLY_KEY, String(enabled));
      setForceMock(enabled);
      setState((current) => ({ ...current, testOnlyMode: enabled }));
      // Reload catalog for the new source mode.
      void (async () => {
        setState((current) => ({ ...current, loading: true, errorMessage: null }));
        try {
          if (DATA_SOURCE === 'REMOTE' && !enabled) {
            const token = await getAccessToken();
            if (!token) {
              setState((current) => ({
                ...current,
                loading: false,
                catalog: null,
                errorMessage: null,
              }));
              return;
            }
          }
          const catalog = await getCatalog();
          setState((current) => ({ ...current, loading: false, catalog }));
          await refreshClientOrders();
        } catch (error) {
          setState((current) => ({
            ...current,
            loading: false,
            errorMessage: error instanceof Error ? error.message : 'No pudimos cargar el catálogo.',
          }));
        }
      })();
    },
    [refreshClientOrders],
  );

  const filteredProducts = useMemo(() => {
    if (!state.catalog) {
      return [];
    }
    const query = state.searchQuery.trim().toLowerCase();
    return state.catalog.products.filter((product) => {
      const matchesCategory =
        state.selectedCategoryId === null || product.categoryId === state.selectedCategoryId;
      const matchesQuery =
        !query ||
        product.name.toLowerCase().includes(query) ||
        product.description.toLowerCase().includes(query);
      return product.available && matchesCategory && matchesQuery;
    });
  }, [state.catalog, state.searchQuery, state.selectedCategoryId]);

  const selectedProduct = useMemo(() => {
    if (!state.catalog || state.selectedProductId === null) {
      return null;
    }
    return state.catalog.products.find((product) => product.id === state.selectedProductId) ?? null;
  }, [state.catalog, state.selectedProductId]);

  const isSelectedProductValid = useMemo(() => {
    if (!selectedProduct) {
      return false;
    }
    try {
      validateSelections(selectedProduct, state.selectedOptionIds);
      return state.selectedQuantity >= 1 && state.selectedQuantity <= 20;
    } catch {
      return false;
    }
  }, [selectedProduct, state.selectedOptionIds, state.selectedQuantity]);

  const selectedProductPreviewPrice = selectedProduct
    ? productUnitPreview(selectedProduct, state.selectedOptionIds)
    : '0.00';

  const selectedProductPreviewTotal = selectedProductPreviewPrice;

  const cartCount = state.cartLines.reduce((sum, line) => sum + line.quantity, 0);
  const cartTotal = cartPreview(state.cartLines);

  const hasEnoughBalance =
    state.checkoutPayment !== 'saldo' ||
    parseMoney(wallet.balance).gte(parseMoney(cartTotal));

  const canCreateOrder =
    state.cartLines.length > 0 &&
    !state.submitting &&
    hasEnoughBalance &&
    (state.testOnlyMode || DATA_SOURCE === 'MOCK' || state.checkoutPayment === 'efectivo');

  const selectedOrder = useMemo(() => {
    if (!state.selectedOrderId) {
      return state.clientOrders[0] ?? null;
    }
    return state.clientOrders.find((order) => order.summary.id === state.selectedOrderId) ?? null;
  }, [state.clientOrders, state.selectedOrderId]);

  const setSearchQuery = useCallback((value: string) => {
    setState((current) => ({ ...current, searchQuery: value }));
  }, []);

  const setSelectedCategoryId = useCallback((value: number | null) => {
    setState((current) => ({ ...current, selectedCategoryId: value }));
  }, []);

  const openProduct = useCallback(
    (productId: number) => {
      const product = state.catalog?.products.find((item) => item.id === productId);
      if (!product) {
        return;
      }
      setState((current) => ({
        ...current,
        selectedProductId: productId,
        selectedOptionIds: getDefaultSelections(product),
        selectedQuantity: 1,
      }));
    },
    [state.catalog],
  );

  const closeProduct = useCallback(() => {
    setState((current) => ({
      ...current,
      selectedProductId: null,
      selectedOptionIds: [],
      selectedQuantity: 1,
    }));
  }, []);

  const toggleOption = useCallback((groupId: number, optionId: number) => {
    setState((current) => {
      const product = current.catalog?.products.find((item) => item.id === current.selectedProductId);
      if (!product) {
        return current;
      }
      const group = product.optionGroups.find((item) => item.id === groupId);
      if (!group) {
        return current;
      }

      const selected = new Set(current.selectedOptionIds);
      if (group.maximumSelections === 1) {
        group.options.forEach((option) => selected.delete(option.id));
        selected.add(optionId);
      } else if (selected.has(optionId)) {
        selected.delete(optionId);
      } else if (selected.size < group.maximumSelections) {
        selected.add(optionId);
      }

      return { ...current, selectedOptionIds: [...selected] };
    });
  }, []);

  const setSelectedQuantity = useCallback((quantity: number) => {
    setState((current) => ({
      ...current,
      selectedQuantity: Math.max(1, Math.min(20, quantity)),
    }));
  }, []);

  const addSelectedProductToCart = useCallback(() => {
    setState((current) => {
      const product = current.catalog?.products.find((item) => item.id === current.selectedProductId);
      if (!product) {
        return current;
      }
      try {
        validateSelections(product, current.selectedOptionIds);
      } catch {
        return current;
      }

      const key = cartLineKey({
        product,
        selectedOptionIds: current.selectedOptionIds,
      });
      const existing = current.cartLines.find((line) => cartLineKey(line) === key);
      const nextLines = existing
        ? current.cartLines.map((line) =>
            cartLineKey(line) === key
              ? { ...line, quantity: Math.min(20, line.quantity + current.selectedQuantity) }
              : line,
          )
        : [
            ...current.cartLines,
            {
              product,
              quantity: current.selectedQuantity,
              selectedOptionIds: [...current.selectedOptionIds],
            },
          ];

      return {
        ...current,
        cartLines: nextLines,
        selectedProductId: null,
        selectedOptionIds: [],
        selectedQuantity: 1,
      };
    });
  }, []);

  const updateCartQuantity = useCallback((lineKey: string, delta: number) => {
    setState((current) => {
      const nextLines = current.cartLines
        .map((line) => {
          if (cartLineKey(line) !== lineKey) {
            return line;
          }
          const nextQty = line.quantity + delta;
          return { ...line, quantity: nextQty };
        })
        .filter((line) => line.quantity > 0);
      return { ...current, cartLines: nextLines };
    });
  }, []);

  const clearCart = useCallback(() => {
    setState((current) => ({ ...current, cartLines: [], kitchenNotes: '' }));
  }, []);

  const setCartLines = useCallback((lines: CartLine[]) => {
    setState((current) => ({ ...current, cartLines: lines }));
  }, []);

  const setKitchenNotes = useCallback((notes: string) => {
    setState((current) => ({ ...current, kitchenNotes: notes }));
  }, []);

  const setCheckoutDestination = useCallback((destination: OrderDestination) => {
    setState((current) => ({ ...current, checkoutDestination: destination }));
  }, []);

  const setCheckoutPayment = useCallback((payment: PaymentMethod) => {
    setState((current) => ({ ...current, checkoutPayment: payment }));
  }, []);

  const setSelectedSpaceId = useCallback((spaceId: number) => {
    setState((current) => ({ ...current, selectedSpaceId: spaceId }));
  }, []);

  const setCreatedOrder = useCallback((order: OrderDetail | null) => {
    setState((current) => ({ ...current, createdOrder: order }));
  }, []);

  const setClientOrders = useCallback((orders: OrderDetail[], selectedId: string | null) => {
    setState((current) => ({
      ...current,
      clientOrders: orders,
      selectedOrderId: selectedId,
    }));
  }, []);

  const selectOrder = useCallback((orderId: string | null) => {
    setState((current) => ({ ...current, selectedOrderId: orderId }));
  }, []);

  const confirmOrder = useCallback(async () => {
    if (!canCreateOrder) {
      return null;
    }

    if (
      DATA_SOURCE === 'REMOTE' &&
      !state.testOnlyMode &&
      (state.checkoutPayment !== 'efectivo' || state.checkoutDestination !== 'para_llevar')
    ) {
      setState((current) => ({
        ...current,
        submitError:
          'Checkout con saldo, tarjeta o mesa solo está disponible en Solo pruebas / MOCK.',
      }));
      return null;
    }

    const request: CreateOrderRequest = {
      paymentMethod: state.checkoutPayment,
      destination: state.checkoutDestination,
      spaceId: state.checkoutDestination === 'en_espacio' ? state.selectedSpaceId : null,
      kitchenNotes: state.kitchenNotes,
      items: state.cartLines.map((line) => ({
        productId: line.product.id,
        quantity: line.quantity,
        optionIds: line.selectedOptionIds,
      })),
    };

    setState((current) => ({ ...current, submitting: true, submitError: null }));
    try {
      if (state.checkoutPayment === 'saldo') {
        await wallet.debitForOrder(cartTotal);
      }

      const idempotencyKey = await generateIdempotencyKey();
      const order = await createStudentCheckout(request, idempotencyKey);
      setState((current) => ({
        ...current,
        submitting: false,
        createdOrder: order,
        cartLines: [],
        kitchenNotes: '',
        clientOrders: [order, ...current.clientOrders.filter((item) => item.summary.id !== order.summary.id)],
        selectedOrderId: order.summary.id,
      }));
      return order;
    } catch (error) {
      setState((current) => ({
        ...current,
        submitting: false,
        submitError: error instanceof Error ? error.message : 'No pudimos crear el pedido.',
      }));
      return null;
    }
  }, [
    canCreateOrder,
    cartTotal,
    state.cartLines,
    state.checkoutDestination,
    state.checkoutPayment,
    state.kitchenNotes,
    state.selectedSpaceId,
    state.testOnlyMode,
    wallet,
  ]);

  const clearCreatedOrder = useCallback(() => {
    setState((current) => ({ ...current, createdOrder: null }));
  }, []);

  const value = useMemo<OrderFlowContextValue>(
    () => ({
      ...state,
      cartCount,
      cartTotal,
      filteredProducts,
      selectedProduct,
      isSelectedProductValid,
      selectedProductPreviewPrice,
      selectedProductPreviewTotal,
      canCreateOrder,
      selectedOrder,
      loadCatalog,
      refreshClientOrders,
      setSearchQuery,
      setSelectedCategoryId,
      openProduct,
      closeProduct,
      toggleOption,
      setSelectedQuantity,
      addSelectedProductToCart,
      updateCartQuantity,
      clearCart,
      setCartLines,
      setKitchenNotes,
      setCheckoutDestination,
      setCheckoutPayment,
      setSelectedSpaceId,
      setCreatedOrder,
      setClientOrders,
      selectOrder,
      confirmOrder,
      clearCreatedOrder,
      setTestOnlyMode,
    }),
    [
      state,
      cartCount,
      cartTotal,
      filteredProducts,
      selectedProduct,
      isSelectedProductValid,
      selectedProductPreviewPrice,
      selectedProductPreviewTotal,
      canCreateOrder,
      selectedOrder,
      loadCatalog,
      refreshClientOrders,
      setSearchQuery,
      setSelectedCategoryId,
      openProduct,
      closeProduct,
      toggleOption,
      setSelectedQuantity,
      addSelectedProductToCart,
      updateCartQuantity,
      clearCart,
      setCartLines,
      setKitchenNotes,
      setCheckoutDestination,
      setCheckoutPayment,
      setSelectedSpaceId,
      setCreatedOrder,
      setClientOrders,
      selectOrder,
      confirmOrder,
      clearCreatedOrder,
      setTestOnlyMode,
    ],
  );

  return <OrderFlowContext.Provider value={value}>{children}</OrderFlowContext.Provider>;
}

export function useOrderFlow(): OrderFlowContextValue {
  const context = useContext(OrderFlowContext);
  if (!context) {
    throw new Error('useOrderFlow must be used within OrderFlowProvider');
  }
  return context;
}
