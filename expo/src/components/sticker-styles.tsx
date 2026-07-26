import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PAYMENT_LABELS, moneyLabel } from '@/domain/models';
import { colors } from '@/theme/colors';
import { fontFamily, weight } from '@/theme/typography';

/** Las seis variantes de .ticket-receipt del demo (pantallas 51 a 56). */
export const STICKER_STYLES = [
  { id: 0, label: 'Editorial' },
  { id: 1, label: 'Core' },
  { id: 2, label: 'Limited' },
  { id: 3, label: 'Breakfast' },
  { id: 4, label: 'QR Live' },
  { id: 5, label: 'Térmico' },
] as const;

export interface StickerOrderData {
  folio: number;
  total: string;
  productName: string;
  paymentLabel: string;
  destinationLabel: string;
  date: string;
}

interface StickerStyleContentProps {
  styleId: number;
  order: StickerOrderData;
}

export function StickerStyleContent({ styleId, order }: StickerStyleContentProps) {
  switch (styleId) {
    case 0:
      return <EditorialSticker order={order} />;
    case 1:
      return <CoreSticker order={order} />;
    case 2:
      return <LimitedSticker order={order} />;
    case 3:
      return <BreakfastSticker order={order} />;
    case 4:
      return <QrLiveSticker order={order} />;
    default:
      return <ThermalSticker order={order} />;
  }
}

/* --------------------------------------------------------------------------
 * Piezas compartidas
 * ----------------------------------------------------------------------- */

/** `.ticket-barcode { height:66 }` portado como barras de ancho variable. */
function Barcode({ color, height = 66 }: { color: string; height?: number }) {
  const widths = [2, 1, 4, 1, 2, 3, 1, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 2, 1, 3];
  return (
    <View style={[styles.barcode, { height }]}>
      {widths.map((width, index) => (
        <View
          key={`${width}-${index}`}
          style={{
            width,
            height: '100%',
            backgroundColor: index % 2 === 0 ? color : 'transparent',
            opacity: 0.92,
          }}
        />
      ))}
    </View>
  );
}

/** `.ticket-topline { borde inferior currentColor; padding-bottom:8 }` */
function Topline({
  brand,
  serial,
  color,
}: {
  brand: string;
  serial: string;
  color: string;
}) {
  return (
    <View style={[styles.topline, { borderBottomColor: color }]}>
      <Text style={[styles.brand, { color }]}>{brand}</Text>
      <Text style={[styles.serial, { color }]}>{serial}</Text>
    </View>
  );
}

/** `.ticket-meta` de tres celdas con bordes arriba y abajo. */
function MetaRow({
  cells,
  color,
}: {
  cells: Array<{ value: string; label: string }>;
  color: string;
}) {
  return (
    <View style={[styles.meta, { borderTopColor: color, borderBottomColor: color }]}>
      {cells.map((cell, index) => (
        <View
          key={cell.label}
          style={[
            styles.metaCell,
            index > 0 ? { borderLeftWidth: 1, borderLeftColor: color } : null,
          ]}
        >
          <Text style={[styles.metaValue, { color }]} numberOfLines={1}>
            {cell.value}
          </Text>
          <Text style={[styles.metaLabel, { color }]}>{cell.label}</Text>
        </View>
      ))}
    </View>
  );
}

/** `.ticket-item { grid 20px 1fr auto }` */
function TicketItem({
  quantity,
  name,
  detail,
  price,
  color,
}: {
  quantity: string;
  name: string;
  detail?: string;
  price: string;
  color: string;
}) {
  return (
    <View style={styles.item}>
      <Text style={[styles.itemStrong, { color, width: 20 }]}>{quantity}</Text>
      <View style={styles.flex}>
        <Text style={[styles.itemText, { color }]}>{name}</Text>
        {detail ? <Text style={[styles.itemSmall, { color }]}>{detail}</Text> : null}
      </View>
      <Text style={[styles.itemStrong, { color }]}>{price}</Text>
    </View>
  );
}

/** `.ticket-token-row` con la etiqueta redonda `.ticket-token`. */
function TokenRow({
  left,
  token,
  color,
  style,
}: {
  left: string;
  token: string;
  color: string;
  style?: object;
}) {
  return (
    <View style={[styles.tokenRow, style]}>
      <Text style={[styles.micro, { color }]}>{left}</Text>
      <View style={[styles.token, { borderColor: color }]}>
        <Text style={[styles.tokenText, { color }]}>{token}</Text>
      </View>
    </View>
  );
}

/* --------------------------------------------------------------------------
 * 0. Editorial (pantalla 51)
 * ----------------------------------------------------------------------- */

function EditorialSticker({ order }: { order: StickerOrderData }) {
  const ink = '#f4f4f1';
  return (
    <View style={[styles.receipt, styles.editorial]}>
      <View style={styles.content}>
        {/* .ticket-display { 39px; line-height:.86 } */}
        <Text style={[styles.display, { color: ink }]}>ANTOJO</Text>

        {/* .ticket-size-row */}
        <View style={styles.sizeRow}>
          {['XS', 'S', 'M', 'XL', 'XXL'].map((size) => (
            <Text key={size} style={[styles.sizeText, { color: ink }]}>
              {size}
            </Text>
          ))}
        </View>

        {/* .ticket-editorial-mid { margin-top:105; dos columnas } */}
        <View style={styles.editorialMid}>
          <View style={styles.editorialMidLeft}>
            <Barcode color={ink} />
            <Text style={[styles.code, { color: ink }]}>1 200220 190045</Text>
          </View>
          <View style={styles.editorialMidRight}>
            <View style={[styles.stamp, { borderColor: ink }]}>
              <Text style={[styles.stampText, { color: ink }]}>VNNL</Text>
            </View>
            <Text style={[styles.micro, { color: ink }]}>
              Daily food and type exploration.{'\n'}School edition 01.
            </Text>
          </View>
        </View>

        {/* .ticket-editorial-title */}
        <Text style={[styles.editorialTitle, { color: ink }]}>
          {order.productName}
          {'\n'}
          <Text style={styles.editorialTitleBold}>Product Sticker</Text>
        </Text>

        {/* .ticket-icons con .ticket-glyph */}
        <View style={styles.icons}>
          {['✦', '☼', '◉', '⌁'].map((glyph) => (
            <View key={glyph} style={[styles.glyph, { borderColor: ink }]}>
              <Text style={[styles.glyphText, { color: ink }]}>{glyph}</Text>
            </View>
          ))}
        </View>

        <MetaRow
          color={ink}
          cells={[
            { value: order.date, label: 'FECHA' },
            { value: `#${order.folio}`, label: 'PEDIDO' },
            { value: moneyLabel(order.total), label: 'TOTAL' },
          ]}
        />
        <MetaRow
          color={ink}
          cells={[
            { value: order.paymentLabel.toUpperCase(), label: 'PAGO' },
            { value: order.destinationLabel.toUpperCase(), label: 'DESTINO' },
            { value: 'DARK', label: 'SERIE' },
          ]}
        />

        <Text style={[styles.instructions, { color: ink }]}>
          PRODUCT INSTRUCTIONS AND GUIDES
        </Text>

        {/* .ticket-footer { borde currentColor; 6px; mayusculas } */}
        <View style={[styles.footer, { borderColor: ink }]}>
          <Text style={[styles.footerText, { color: ink }]}>
            CONSERVA ESTE TICKET PARA CONSULTAR TU PEDIDO. PRESENTA EL CÓDIGO AL RECOGER.
            ESTE STICKER PERTENECE A LA SERIE ESCOLAR 01.
          </Text>
        </View>
      </View>
    </View>
  );
}

/* --------------------------------------------------------------------------
 * 1. Core (pantalla 52)
 * ----------------------------------------------------------------------- */

function CoreSticker({ order }: { order: StickerOrderData }) {
  const ink = colors.accentInk;
  return (
    <View style={[styles.receipt, styles.core]}>
      <View style={styles.content}>
        <Topline brand="VAIINILLA" serial={'RECEIPT STICKER\nDROP 024'} color={ink} />

        {/* .ticket-core-headline { 49px; line-height:.82 } */}
        <Text style={[styles.coreHeadline, { color: ink }]}>YA ES{'\n'}TUYO.</Text>

        {/* .ticket-status { ink; radius:12 } */}
        <View style={styles.status}>
          <Text style={styles.statusText}>● PEDIDO PAGADO</Text>
        </View>

        {/* .ticket-product-block { rgba blanca .34; radius:20 } */}
        <View style={styles.productBlock}>
          <Text style={[styles.label, { color: ink }]}>Tu pedido</Text>
          <Text style={[styles.productName, { color: ink }]}>{order.productName}</Text>
          <Text style={[styles.micro, { color: ink }]}>
            Asada · salsa verde · queso gratinado
          </Text>
        </View>

        <MetaRow
          color={ink}
          cells={[
            { value: `#${order.folio}`, label: 'ORDEN' },
            { value: order.destinationLabel.toUpperCase(), label: 'DESTINO' },
            { value: '12:48', label: 'HORA' },
          ]}
        />

        {/* .ticket-collection */}
        <View style={styles.collection}>
          {['✦', '◉', '⌁', 'V'].map((glyph) => (
            <Text key={glyph} style={[styles.collectionGlyph, { color: ink }]}>
              {glyph}
            </Text>
          ))}
        </View>

        {/* .ticket-total { borde superior } */}
        <View style={[styles.total, { borderTopColor: ink }]}>
          <View>
            <Text style={[styles.label, { color: ink }]}>Total pagado</Text>
            <Text style={[styles.micro, { color: ink }]}>{order.paymentLabel}</Text>
          </View>
          <Text style={[styles.totalValue, { color: ink }]}>{moneyLabel(order.total)}</Text>
        </View>

        <View style={styles.spacer16}>
          <Barcode color={ink} />
          <TokenRow
            color={ink}
            left={`VNL-${order.folio}-101MX`}
            token="COMMON 01/24"
            style={styles.marginTop6}
          />
        </View>
      </View>
    </View>
  );
}

/* --------------------------------------------------------------------------
 * 2. Limited (pantalla 53)
 * ----------------------------------------------------------------------- */

function LimitedSticker({ order }: { order: StickerOrderData }) {
  const ink = '#2d100e';
  return (
    <View style={[styles.receipt, styles.limited]}>
      <View style={styles.content}>
        <Topline brand="VNL / DROP" serial={'LIMITED\nEDITION'} color={ink} />

        {/* .ticket-drop { 65px; line-height:.75 } */}
        <Text style={[styles.drop, { color: ink }]}>HOT{'\n'}LUNCH</Text>
        <Text style={[styles.micro, { color: ink }]}>
          Edición especial desbloqueada por comprar el producto de la semana.
        </Text>

        {/* .ticket-cut { bordes punteados } */}
        <View style={[styles.cut, { borderTopColor: ink, borderBottomColor: ink }]} />

        <Text style={[styles.label, { color: ink }]}>Objeto desbloqueado</Text>

        {/* .ticket-rare */}
        <View style={styles.rare}>
          <View style={styles.flex}>
            <Text style={[styles.rareTitle, { color: ink }]}>
              {order.productName}
              {'\n'}Serie fuego 02
            </Text>
            <Text style={[styles.micro, { color: ink }]}>Disponible del 13 al 17 de julio.</Text>
          </View>
          <View style={styles.star}>
            <Text style={styles.starText}>✹</Text>
          </View>
        </View>

        <MetaRow
          color={ink}
          cells={[
            { value: `#${order.folio}`, label: 'ORDEN' },
            { value: moneyLabel(order.total), label: 'TOTAL' },
            { value: 'RARE', label: 'NIVEL' },
          ]}
        />

        <View style={styles.items}>
          <TicketItem
            quantity="01"
            name={order.productName}
            detail="asadero · chile pasado"
            price={moneyLabel(order.total)}
            color={ink}
          />
        </View>

        <Barcode color={ink} />
        <TokenRow
          color={ink}
          left="SERIE 02 · 013/150"
          token="LIMITED"
          style={styles.marginTop7}
        />

        <View style={[styles.footer, styles.marginTop12, { borderColor: ink }]}>
          <Text style={[styles.footerText, { color: ink }]}>
            GUARDA ESTE STICKER EN TU COLECCIÓN. COMPLETA TRES DE LA SERIE FUEGO PARA
            DESBLOQUEAR UNA RECOMPENSA.
          </Text>
        </View>
      </View>
    </View>
  );
}

/* --------------------------------------------------------------------------
 * 3. Breakfast (pantalla 54)
 * ----------------------------------------------------------------------- */

function BreakfastSticker({ order }: { order: StickerOrderData }) {
  const ink = '#2a210c';
  return (
    <View style={[styles.receipt, styles.breakfast]}>
      <View style={styles.content}>
        <Topline brand="VAIINILLA AM" serial={'BREAKFAST\nCLUB 2026'} color={ink} />

        {/* .ticket-sun { 94px; circulo yolk } */}
        <View style={styles.sun}>
          <Text style={styles.sunText}>☀</Text>
        </View>

        <Text style={[styles.breakfastTitle, { color: ink }]}>
          DESAYUNO{'\n'}COMPLETO.
        </Text>

        <MetaRow
          color={ink}
          cells={[
            { value: '08:14', label: 'HORA' },
            { value: `#${order.folio}`, label: 'ORDEN' },
            { value: moneyLabel(order.total), label: 'TOTAL' },
          ]}
        />

        <View style={styles.items}>
          <TicketItem
            quantity="01"
            name="Mollete especial"
            detail="frijol · queso · pico de gallo"
            price="$45"
            color={ink}
          />
          <TicketItem quantity="01" name="Agua natural" price="$7" color={ink} />
        </View>

        {/* .ticket-coupon { borde punteado 2px; radius:18 } */}
        <View style={[styles.coupon, { borderColor: ink }]}>
          <Text style={[styles.label, { color: ink }]}>Progreso de mañana</Text>
          <Text style={[styles.couponValue, { color: ink }]}>3 / 5</Text>
          <Text style={[styles.micro, styles.centered, { color: ink }]}>
            Dos desayunos más para desbloquear recarga bonus.
          </Text>
        </View>

        <Barcode color={ink} />
        <Text style={[styles.code, { color: ink }]}>MORNING-{order.folio}-0305</Text>
      </View>
    </View>
  );
}

/* --------------------------------------------------------------------------
 * 4. QR Live / digital (pantalla 55)
 * ----------------------------------------------------------------------- */

/** Patron fijo de 7x7 para el `.ticket-qr` (el demo lo dibuja con <i>). */
const QR_PATTERN = [
  1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0,
  1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1,
];

function QrLiveSticker({ order }: { order: StickerOrderData }) {
  const ink = '#f5f1e5';
  return (
    <View style={[styles.receipt, styles.digital]}>
      <View style={styles.content}>
        <Topline brand="VNL://ORDER" serial={`LIVE RECEIPT\n${order.date}`} color={ink} />

        <Text style={styles.digitalTitle}>READY{'\n'}TO PICK.</Text>

        {/* .ticket-live con .ticket-pulse */}
        <View style={styles.live}>
          <View style={styles.pulse} />
          <Text style={[styles.liveText, { color: ink }]}>PEDIDO LISTO · CAJA 02</Text>
        </View>

        {/* .ticket-terminal */}
        <View style={styles.terminal}>
          {[
            { key: 'ORDER_ID', value: `#${order.folio}` },
            { key: 'PRODUCT', value: order.productName.toUpperCase().replace(/ /g, '_') },
            { key: 'PAYMENT', value: order.paymentLabel.toUpperCase().replace(/ /g, '_') },
            { key: 'DESTINATION', value: order.destinationLabel.toUpperCase().replace(/ /g, '_') },
            { key: 'TOTAL_MXN', value: order.total },
          ].map((row) => (
            <View key={row.key} style={styles.terminalRow}>
              <Text style={[styles.terminalKey, { color: ink }]}>{row.key}</Text>
              <Text style={[styles.terminalValue, { color: ink }]} numberOfLines={1}>
                {row.value}
              </Text>
            </View>
          ))}
        </View>

        {/* .ticket-qr { 105x105; rejilla 7x7 } */}
        <View style={styles.qr}>
          {QR_PATTERN.map((cell, index) => (
            <View
              key={index}
              style={[styles.qrCell, cell ? styles.qrCellOn : styles.qrCellOff]}
            />
          ))}
        </View>
      </View>
    </View>
  );
}

/* --------------------------------------------------------------------------
 * 5. Termico (pantalla 56)
 * ----------------------------------------------------------------------- */

function ThermalSticker({ order }: { order: StickerOrderData }) {
  const ink = '#141414';
  return (
    <View style={[styles.receipt, styles.thermal]}>
      <View style={styles.content}>
        <Text style={[styles.thermalBrand, { color: ink }]}>VAIINILLA</Text>
        <Text style={[styles.thermalCenter, { color: ink }]}>
          COMEDOR ESCOLAR · CAMPUS CHIHUAHUA
        </Text>

        <View style={[styles.dashed, { borderTopColor: ink }]} />

        <View style={styles.thermalRow}>
          <Text style={[styles.thermalMono, { color: ink }]}>ORDEN</Text>
          <Text style={[styles.thermalMonoBold, { color: ink }]}>#{order.folio}</Text>
        </View>
        <View style={styles.thermalRow}>
          <Text style={[styles.thermalMono, { color: ink }]}>FECHA</Text>
          <Text style={[styles.thermalMonoBold, { color: ink }]}>{order.date}</Text>
        </View>
        <View style={styles.thermalRow}>
          <Text style={[styles.thermalMono, { color: ink }]}>PAGO</Text>
          <Text style={[styles.thermalMonoBold, { color: ink }]}>
            {order.paymentLabel.toUpperCase()}
          </Text>
        </View>
        <View style={styles.thermalRow}>
          <Text style={[styles.thermalMono, { color: ink }]}>DESTINO</Text>
          <Text style={[styles.thermalMonoBold, { color: ink }]}>
            {order.destinationLabel.toUpperCase()}
          </Text>
        </View>

        <View style={[styles.dashed, { borderTopColor: ink }]} />

        <TicketItem
          quantity="01"
          name={order.productName}
          price={moneyLabel(order.total)}
          color={ink}
        />

        <View style={[styles.dashed, { borderTopColor: ink }]} />

        <View style={styles.thermalRow}>
          <Text style={[styles.thermalTotalLabel, { color: ink }]}>TOTAL</Text>
          <Text style={[styles.thermalTotal, { color: ink }]}>{moneyLabel(order.total)}</Text>
        </View>

        <View style={styles.spacer16}>
          <Barcode color={ink} height={52} />
          <Text style={[styles.code, styles.centered, { color: ink }]}>
            VNL-{order.folio}-TERM
          </Text>
        </View>

        <Text style={[styles.thermalCenter, { color: ink }]}>GRACIAS POR TU COMPRA</Text>
      </View>
    </View>
  );
}

export function orderToStickerData(order: {
  summary: { folio: number; total: string; paymentMethod: keyof typeof PAYMENT_LABELS };
  items: Array<{ productName: string }>;
  summaryDestination?: string;
}): StickerOrderData {
  return {
    folio: order.summary.folio,
    total: order.summary.total,
    productName: order.items[0]?.productName ?? 'Pedido',
    paymentLabel: PAYMENT_LABELS[order.summary.paymentMethod],
    destinationLabel: 'Para llevar',
    date: '23/07/26',
  };
}

const styles = StyleSheet.create({
  flex: { flex: 1, minWidth: 0 },
  centered: { textAlign: 'center' },
  spacer16: { marginTop: 16 },
  marginTop6: { marginTop: 6 },
  marginTop7: { marginTop: 7 },
  marginTop12: { marginTop: 12 },

  // .ticket-receipt { min-height:610 } / .ticket-content { padding:16 }
  receipt: { width: '100%', minHeight: 610, overflow: 'hidden' },
  content: { padding: 16, flex: 1 },

  // Fondos por variante
  editorial: {
    backgroundColor: '#20211f',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.72)',
  },
  core: { backgroundColor: colors.accent, borderRadius: 24 },
  limited: {
    backgroundColor: colors.coral,
    borderRadius: 10,
    transform: [{ rotate: '-0.7deg' }],
  },
  breakfast: { backgroundColor: '#fff7dc' },
  digital: {
    backgroundColor: '#070807',
    borderRadius: 27,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  thermal: { backgroundColor: '#fffef8', borderWidth: 1, borderColor: 'rgba(0,0,0,0.12)' },

  // .ticket-topline
  topline: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
    borderBottomWidth: 1,
    paddingBottom: 8,
  },
  brand: { fontFamily, fontSize: 18, fontWeight: weight.black, letterSpacing: -0.99, lineHeight: 17 },
  serial: {
    fontFamily,
    fontSize: 7,
    fontWeight: weight.black,
    letterSpacing: 0.7,
    textAlign: 'right',
    lineHeight: 10,
  },

  // .ticket-display / .ticket-size-row
  display: { fontFamily, fontSize: 39, lineHeight: 39 * 0.86, letterSpacing: -2.9, fontWeight: weight.black },
  sizeRow: { flexDirection: 'row', gap: 6, flexWrap: 'wrap', marginTop: 7 },
  sizeText: { fontFamily, fontSize: 10, fontWeight: weight.bold },

  // .ticket-editorial-mid
  editorialMid: { flexDirection: 'row', gap: 13, alignItems: 'flex-end', marginTop: 105 },
  editorialMidLeft: { flex: 1.16 },
  editorialMidRight: { flex: 0.84, gap: 6 },
  editorialTitle: {
    fontFamily,
    fontSize: 19,
    lineHeight: 19 * 1.05,
    textAlign: 'center',
    marginTop: 10,
    marginBottom: 12,
  },
  editorialTitleBold: { fontWeight: weight.black },

  // .ticket-stamp / .ticket-micro
  stamp: {
    height: 52,
    borderWidth: 2,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stampText: { fontFamily, fontSize: 20, letterSpacing: 1, fontWeight: weight.black },
  micro: { fontFamily, fontSize: 7, lineHeight: 7 * 1.38 },

  // .ticket-barcode / .ticket-code
  barcode: { flexDirection: 'row', overflow: 'hidden' },
  code: { fontFamily, fontSize: 8, letterSpacing: 1.36, marginTop: 4 },

  // .ticket-icons / .ticket-glyph
  icons: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 },
  glyph: {
    width: 29,
    height: 29,
    borderWidth: 1,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
  },
  glyphText: { fontFamily, fontSize: 12, fontWeight: weight.black },

  // .ticket-meta / .ticket-meta-cell
  meta: { flexDirection: 'row', borderTopWidth: 1, borderBottomWidth: 1 },
  metaCell: { flex: 1, minWidth: 0, paddingVertical: 8, paddingHorizontal: 5, alignItems: 'center' },
  metaValue: { fontFamily, fontSize: 11, fontWeight: weight.black },
  metaLabel: { fontFamily, fontSize: 6, letterSpacing: 0.8, fontWeight: weight.bold, opacity: 0.7 },

  instructions: { fontFamily, fontSize: 10, marginTop: 10, marginBottom: 5, textAlign: 'center' },

  // .ticket-footer
  footer: { borderWidth: 1, padding: 7 },
  footerText: { fontFamily, fontSize: 6, lineHeight: 6 * 1.35, textAlign: 'center' },

  // Core
  coreHeadline: {
    fontFamily,
    fontSize: 49,
    lineHeight: 49 * 0.82,
    letterSpacing: -4.1,
    fontWeight: weight.black,
    marginTop: 22,
    marginBottom: 16,
  },
  status: {
    alignSelf: 'flex-start',
    borderRadius: 12,
    paddingVertical: 8,
    paddingHorizontal: 10,
    backgroundColor: '#171817',
  },
  statusText: { fontFamily, fontSize: 8, fontWeight: weight.black, letterSpacing: 0.64, color: '#fff' },
  productBlock: {
    backgroundColor: 'rgba(255,255,255,0.34)',
    borderRadius: 20,
    padding: 13,
    marginVertical: 15,
  },
  label: { fontFamily, fontSize: 7, letterSpacing: 0.91, fontWeight: weight.black, opacity: 0.65 },
  productName: { fontFamily, fontSize: 15, fontWeight: weight.black, marginVertical: 3 },
  collection: { flexDirection: 'row', gap: 7, marginVertical: 13 },
  collectionGlyph: { fontFamily, fontSize: 14 },
  total: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    gap: 12,
    borderTopWidth: 1,
    paddingTop: 9,
  },
  totalValue: { fontFamily, fontSize: 22, fontWeight: weight.black },
  tokenRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  token: { borderWidth: 1, borderRadius: 999, paddingHorizontal: 8, paddingVertical: 4 },
  tokenText: { fontFamily, fontSize: 7, fontWeight: weight.black, letterSpacing: 0.49 },

  // Limited
  drop: {
    fontFamily,
    fontSize: 65,
    lineHeight: 65 * 0.75,
    letterSpacing: -6.5,
    fontWeight: weight.black,
    marginTop: 18,
    marginBottom: 13,
  },
  cut: { height: 23, marginVertical: 11, borderTopWidth: 1, borderBottomWidth: 1, borderStyle: 'dashed' },
  rare: { flexDirection: 'row', alignItems: 'center', gap: 10, marginVertical: 12 },
  rareTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, lineHeight: 15 },
  star: {
    width: 54,
    height: 54,
    borderRadius: 18,
    backgroundColor: '#171817',
    alignItems: 'center',
    justifyContent: 'center',
  },
  starText: { fontFamily, fontSize: 25, color: '#fff' },

  // .ticket-items / .ticket-item
  items: { gap: 6, paddingVertical: 9 },
  item: { flexDirection: 'row', gap: 6, alignItems: 'flex-start' },
  itemStrong: { fontFamily, fontSize: 8, lineHeight: 10, fontWeight: weight.black },
  itemText: { fontFamily, fontSize: 8, lineHeight: 10 },
  itemSmall: { fontFamily, fontSize: 7, lineHeight: 9, opacity: 0.7 },

  // Breakfast
  sun: {
    width: 94,
    height: 94,
    borderRadius: 47,
    backgroundColor: colors.yolk,
    alignSelf: 'center',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 20,
    marginBottom: 16,
  },
  sunText: { fontFamily, fontSize: 42 },
  breakfastTitle: {
    fontFamily,
    fontSize: 35,
    lineHeight: 35 * 0.9,
    letterSpacing: -2.3,
    fontWeight: weight.black,
    textAlign: 'center',
    marginBottom: 15,
  },
  coupon: {
    borderWidth: 2,
    borderStyle: 'dashed',
    borderRadius: 18,
    padding: 12,
    alignItems: 'center',
    marginVertical: 13,
  },
  couponValue: { fontFamily, fontSize: 22, fontWeight: weight.black, marginVertical: 3 },

  // Digital
  digitalTitle: {
    fontFamily,
    fontSize: 41,
    lineHeight: 41 * 0.88,
    letterSpacing: -3.1,
    fontWeight: weight.black,
    color: '#b9d86d',
    marginTop: 20,
    marginBottom: 15,
  },
  live: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 14 },
  pulse: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#b9d86d' },
  liveText: { fontFamily, fontSize: 8, letterSpacing: 0.96 },
  terminal: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.18)',
    borderRadius: 18,
    padding: 12,
    backgroundColor: 'rgba(255,255,255,0.035)',
  },
  terminalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.1)',
  },
  terminalKey: { fontFamily, fontSize: 7, opacity: 0.7 },
  terminalValue: { fontFamily, fontSize: 7, fontWeight: weight.black },

  // .ticket-qr { 105x105; rejilla 7x7; padding:7 }
  qr: {
    width: 105,
    height: 105,
    padding: 7,
    backgroundColor: '#fff',
    alignSelf: 'center',
    marginTop: 17,
    marginBottom: 12,
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  qrCell: { width: `${100 / 7}%`, height: `${100 / 7}%` },
  qrCellOn: { backgroundColor: '#070807' },
  qrCellOff: { backgroundColor: '#fff' },

  // Termico
  thermalBrand: {
    fontFamily,
    fontSize: 24,
    fontWeight: weight.black,
    letterSpacing: 2,
    textAlign: 'center',
  },
  thermalCenter: { fontFamily, fontSize: 8, letterSpacing: 0.8, textAlign: 'center', marginTop: 4 },
  dashed: { borderTopWidth: 1, borderStyle: 'dashed', marginVertical: 10 },
  thermalRow: { flexDirection: 'row', justifyContent: 'space-between', gap: 10, paddingVertical: 3 },
  thermalMono: { fontFamily, fontSize: 10, letterSpacing: 0.5 },
  thermalMonoBold: { fontFamily, fontSize: 10, letterSpacing: 0.5, fontWeight: weight.black },
  thermalTotalLabel: { fontFamily, fontSize: 13, fontWeight: weight.black, letterSpacing: 1 },
  thermalTotal: { fontFamily, fontSize: 18, fontWeight: weight.black },
});
