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

import { getCatalog } from '@/data/catalog-repository';
import { createOrder, generateIdempotencyKey } from '@/data/order-repository';
import { cartPreview, productUnitPreview, validateSelections } from '@/domain/contract-rules';
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
  submitting: boolean;
  submitError: string | null;
  createdOrder: OrderDetail | null;
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
  loadCatalog: () => Promise<void>;
  setSearchQuery: (value: string) => void;
  setSelectedCategoryId: (value: number | null) => void;
  openProduct: (productId: number) => void;
  closeProduct: () => void;
  toggleOption: (groupId: number, optionId: number) => void;
  setSelectedQuantity: (quantity: number) => void;
  addSelectedProductToCart: () => void;
  updateCartQuantity: (lineKey: string, delta: number) => void;
  setKitchenNotes: (notes: string) => void;
  setCheckoutDestination: (destination: OrderDestination) => void;
  setCheckoutPayment: (payment: PaymentMethod) => void;
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
  submitting: false,
  submitError: null,
  createdOrder: null,
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
  const [state, setState] = useState<OrderFlowState>(defaultState);

  useEffect(() => {
    AsyncStorage.getItem(TEST_ONLY_KEY).then((value) => {
      if (value !== null) {
        setState((current) => ({ ...current, testOnlyMode: value === 'true' }));
      }
    });
    void loadCatalog();
  }, []);

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

  const setTestOnlyMode = useCallback((enabled: boolean) => {
    void AsyncStorage.setItem(TEST_ONLY_KEY, String(enabled));
    setState((current) => ({ ...current, testOnlyMode: enabled }));
  }, []);

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

  const selectedProductPreviewTotal = selectedProduct
    ? productUnitPreview(selectedProduct, state.selectedOptionIds)
    : '0.00';

  const cartCount = state.cartLines.reduce((sum, line) => sum + line.quantity, 0);
  const cartTotal = cartPreview(state.cartLines);

  const canCreateOrder =
    state.cartLines.length > 0 &&
    state.checkoutDestination === 'para_llevar' &&
    state.checkoutPayment === 'efectivo' &&
    !state.submitting;

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

  const setKitchenNotes = useCallback((notes: string) => {
    setState((current) => ({ ...current, kitchenNotes: notes }));
  }, []);

  const setCheckoutDestination = useCallback((destination: OrderDestination) => {
    setState((current) => ({ ...current, checkoutDestination: destination }));
  }, []);

  const setCheckoutPayment = useCallback((payment: PaymentMethod) => {
    setState((current) => ({ ...current, checkoutPayment: payment }));
  }, []);

  const confirmOrder = useCallback(async () => {
    if (!canCreateOrder) {
      return null;
    }

    const request: CreateOrderRequest = {
      paymentMethod: 'efectivo',
      destination: 'para_llevar',
      spaceId: null,
      kitchenNotes: state.kitchenNotes,
      items: state.cartLines.map((line) => ({
        productId: line.product.id,
        quantity: line.quantity,
        optionIds: line.selectedOptionIds,
      })),
    };

    setState((current) => ({ ...current, submitting: true, submitError: null }));
    try {
      const idempotencyKey = await generateIdempotencyKey();
      const order = await createOrder(request, idempotencyKey);
      setState((current) => ({
        ...current,
        submitting: false,
        createdOrder: order,
        cartLines: [],
        kitchenNotes: '',
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
  }, [canCreateOrder, state.cartLines, state.kitchenNotes]);

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
      loadCatalog,
      setSearchQuery,
      setSelectedCategoryId,
      openProduct,
      closeProduct,
      toggleOption,
      setSelectedQuantity,
      addSelectedProductToCart,
      updateCartQuantity,
      setKitchenNotes,
      setCheckoutDestination,
      setCheckoutPayment,
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
      loadCatalog,
      setSearchQuery,
      setSelectedCategoryId,
      openProduct,
      closeProduct,
      toggleOption,
      setSelectedQuantity,
      addSelectedProductToCart,
      updateCartQuantity,
      setKitchenNotes,
      setCheckoutDestination,
      setCheckoutPayment,
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
