import { parseMoney } from '@/domain/contract-rules';
import { moneyLabel, type Product } from '@/domain/models';

export interface AssistantRecommendation {
  productId: number | null;
  name: string;
  meta: string;
  price: string;
  imageUrl: string;
}

export function assistantReply(userMessage: string, products: Product[]): string {
  const normalized = userMessage.trim().toLowerCase();
  if (normalized.includes('gluten')) {
    return glutenReply(products);
  }
  if (normalized.includes('ligero') || normalized.includes('fresco')) {
    return lightReply(products);
  }
  return recommendReply(products);
}

function glutenReply(products: Product[]): string {
  const safe = products.filter((product) => {
    const allergens = product.allergens.toLowerCase();
    return !allergens.includes('gluten') && !allergens.includes('trigo');
  });
  if (safe.length === 0) {
    return 'Prueba frutas o jamaica; confirma alérgenos en cocina.';
  }
  const names = safe
    .slice(0, 2)
    .map((product) => product.name)
    .join(' o ');
  return `Sin gluten detectable: ${names}. Confirma alérgenos en cocina antes de pedir.`;
}

function lightReply(products: Product[]): string {
  const light = products.filter((product) => {
    const name = product.name.toLowerCase();
    return name.includes('jamaica') || name.includes('fruta') || name.includes('agua');
  });
  const picks =
    light.length > 0
      ? light
          .slice(0, 2)
          .map((product) => `${product.name} (${moneyLabel(product.digitalPrice)})`)
          .join(' y ')
      : 'Agua de jamaica o fruta de temporada';
  return `Para algo ligero y fresco te sugiero ${picks}. Perfecto si quieres acompañar sin sentirte pesado.`;
}

function recommendReply(products: Product[]): string {
  const burrito =
    products.find((product) => product.name.toLowerCase().includes('burrito')) ??
    products.find((product) => product.categoryId === 20) ??
    products[0];
  if (!burrito) {
    return 'Explora el menú de hoy: hay opciones rápidas y llenadoras. ¿Buscas algo específico?';
  }
  return `Te recomiendo el ${burrito.name} por ${moneyLabel(burrito.digitalPrice)}. Es de los favoritos del menú y llega en unos ${burrito.estimatedTimeMinutes} min.`;
}

export function filterByChip(chip: string, products: Product[]): AssistantRecommendation[] {
  const available = products.filter((product) => product.available);
  switch (chip) {
    case 'Menos de $60':
      return budgetRecommendations(available);
    case 'Algo ligero':
      return available
        .filter((product) => {
          const name = product.name.toLowerCase();
          return (
            name.includes('jamaica') ||
            name.includes('fruta') ||
            name.includes('agua') ||
            product.estimatedTimeMinutes <= 5
          );
        })
        .slice(0, 3)
        .map(toRecommendation);
    case 'Combo con bebida': {
      const food = available.filter((product) => product.categoryId === 20).slice(0, 2);
      const drink = available.find((product) => product.categoryId === 10);
      return [...food, ...(drink ? [drink] : [])].map(toRecommendation);
    }
    default:
      return defaultRecommendations(available);
  }
}

function budgetRecommendations(products: Product[]): AssistantRecommendation[] {
  const underBudget = products.filter((product) => parseMoney(product.digitalPrice).lte(60));
  const preferred = ['waffle', 'torta', 'quesadilla'];
  const ordered = preferred
    .map((keyword) => underBudget.find((product) => product.name.toLowerCase().includes(keyword)))
    .filter((product): product is Product => Boolean(product));
  const rest = underBudget.filter((product) => !ordered.some((item) => item.id === product.id));
  return [...ordered, ...rest].slice(0, 3).map(toRecommendation);
}

function defaultRecommendations(products: Product[]): AssistantRecommendation[] {
  const demoKeywords = ['torta', 'burrito', 'quesadilla'];
  return demoKeywords
    .map((keyword) => products.find((product) => product.name.toLowerCase().includes(keyword)))
    .filter((product): product is Product => Boolean(product))
    .map(toRecommendation);
}

function toRecommendation(product: Product): AssistantRecommendation {
  const categoryLabel = product.categoryId === 10 ? 'Bebida' : 'Comida';
  const minTime = Math.max(1, product.estimatedTimeMinutes - 2);
  return {
    productId: product.id,
    name: product.name,
    meta: `${minTime}–${product.estimatedTimeMinutes} min · ${categoryLabel}`,
    price: product.digitalPrice,
    imageUrl: product.imageUrl,
  };
}
