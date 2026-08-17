#!/usr/bin/env python3
"""Valida fixtures canónicos sin requerir Android SDK."""

from __future__ import annotations

import json
import re
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "app" / "src" / "test" / "fixtures"
MONEY = re.compile(r"^(0|[1-9]\d*)\.\d{2}$")
UTC = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(name: str) -> dict:
    with (FIXTURES / name).open(encoding="utf-8") as file:
        return json.load(file)


def validate_meta(meta: dict) -> None:
    require(
        set(meta) == {"page", "total_pages", "total_items", "cursor"},
        "Envelope meta inesperado",
    )


def validate_catalog() -> None:
    envelope = load("catalog.json")
    require(envelope.get("error") is None, "catalog.error debe ser null")
    require(set(envelope) == {"data", "meta", "error"}, "Envelope de catálogo inesperado")
    validate_meta(envelope["meta"])

    data = envelope["data"]
    require(set(data) == {"categorias", "productos"}, "catalog.data inesperado")
    categories = data["categorias"]
    products = data["productos"]
    category_ids = {category["id"] for category in categories}

    require(len(category_ids) == len(categories), "IDs de categoría duplicados")
    require(len({product["id"] for product in products}) == len(products), "IDs de producto duplicados")
    require(envelope["meta"]["total_items"] == len(products), "meta.total_items no coincide")

    product_keys = {
        "id",
        "categoria_id",
        "estacion_preparacion",
        "nombre",
        "descripcion",
        "ingredientes",
        "alergenos",
        "tiempo_estimado_min",
        "precio_mostrador",
        "precio_digital",
        "disponible",
        "imagen_url",
        "grupos_opcion",
    }

    for product in products:
        require(set(product) == product_keys, f"Campos inesperados en producto {product.get('id')}")
        require(product["categoria_id"] in category_ids, "categoria_id inexistente")
        require(product["estacion_preparacion"] in {"caja", "cocina"}, "estación inválida")
        require(MONEY.fullmatch(product["precio_mostrador"]) is not None, "precio_mostrador inválido")
        require(MONEY.fullmatch(product["precio_digital"]) is not None, "precio_digital inválido")
        require(isinstance(product["imagen_url"], str) and product["imagen_url"], "imagen_url inválida")

        group_ids: set[int] = set()
        option_ids: set[int] = set()
        for group in product["grupos_opcion"]:
            require(group["id"] not in group_ids, "ID de grupo duplicado dentro del producto")
            group_ids.add(group["id"])
            minimum = group["min_selecciones"]
            maximum = group["max_selecciones"]
            options = group["opciones"]
            require(0 <= minimum <= maximum <= len(options), "límites de opciones inválidos")
            for option in options:
                require(option["id"] not in option_ids, "ID de opción duplicado dentro del producto")
                option_ids.add(option["id"])
                require(MONEY.fullmatch(option["precio_extra"]) is not None, "precio_extra inválido")


def validate_operational_status() -> None:
    envelope = load("operational_status.json")
    require(envelope.get("error") is None, "operational_status.error debe ser null")
    require(set(envelope) == {"data", "meta", "error"}, "Envelope operativo inesperado")
    validate_meta(envelope["meta"])
    status = envelope["data"]
    expected = {
        "recibiendo_pedidos",
        "sesion_caja_abierta",
        "caja_en_linea",
        "cocina_en_linea",
        "tiempo_estimado_min",
        "consultado_en",
    }
    require(set(status) == expected, "Campos inesperados en OperationalStatus")
    require(status["tiempo_estimado_min"] >= 0, "tiempo_estimado_min negativo")
    require(UTC.fullmatch(status["consultado_en"]) is not None, "consultado_en no es ISO 8601 UTC")


def validate_created_order() -> None:
    envelope = load("created_order.json")
    require(envelope.get("error") is None, "created_order.error debe ser null")
    require(set(envelope) == {"data", "meta", "error"}, "Envelope de pedido inesperado")
    validate_meta(envelope["meta"])
    order = envelope["data"]
    expected = {
        "id",
        "folio",
        "fecha_operativa",
        "estado",
        "metodo_pago",
        "destino",
        "espacio",
        "subtotal",
        "ahorro_combinado",
        "cashback_otorgado",
        "total",
        "version",
        "creado_en",
        "actualizado_en",
        "notas_cocina",
        "items",
    }
    require(set(order) == expected, "Campos inesperados en OrderDetail fixture")
    uuid.UUID(order["id"])
    require(order["estado"] == "por_cobrar", "El pedido nuevo debe iniciar por_cobrar")
    require(order["metodo_pago"] == "efectivo", "El fixture debe usar efectivo")
    require(order["destino"] == "para_llevar", "El fixture debe usar para_llevar")
    require(order["espacio"] is None, "para_llevar exige espacio null")
    require(order["version"] == 1, "El pedido recién creado debe iniciar en versión 1")
    require(UTC.fullmatch(order["creado_en"]) is not None, "creado_en inválido")
    require(UTC.fullmatch(order["actualizado_en"]) is not None, "actualizado_en inválido")
    for field in ("subtotal", "ahorro_combinado", "cashback_otorgado", "total"):
        require(MONEY.fullmatch(order[field]) is not None, f"{field} inválido")
    require(1 <= len(order["items"]) <= 50, "OrderDetail debe contener 1 a 50 items")
    for item in order["items"]:
        require(1 <= item["cantidad"] <= 20, "cantidad fuera de contrato")
        require(MONEY.fullmatch(item["precio_digital_unitario"]) is not None, "precio unitario inválido")
        require(MONEY.fullmatch(item["subtotal"]) is not None, "subtotal de item inválido")
        for option in item["opciones"]:
            require(MONEY.fullmatch(option["precio_extra"]) is not None, "precio_extra de snapshot inválido")


if __name__ == "__main__":
    validate_catalog()
    validate_operational_status()
    validate_created_order()
    print("Fixtures contractuales válidos.")
