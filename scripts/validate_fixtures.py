#!/usr/bin/env python3
"""Validación rápida de los fixtures canónicos de VAI-5 sin Android SDK."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "app" / "src" / "main" / "assets" / "fixtures"
MONEY = re.compile(r"^(0|[1-9]\d*)\.\d{2}$")
UTC = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(name: str) -> dict:
    with (FIXTURES / name).open(encoding="utf-8") as file:
        return json.load(file)


def validate_catalog() -> None:
    envelope = load("catalog.json")
    require(envelope.get("error") is None, "catalog.error debe ser null")
    require(set(envelope) == {"data", "meta", "error"}, "Envelope de catálogo inesperado")

    data = envelope["data"]
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

        for group in product["grupos_opcion"]:
            minimum = group["min_selecciones"]
            maximum = group["max_selecciones"]
            options = group["opciones"]
            require(0 <= minimum <= maximum <= len(options), "límites de opciones inválidos")
            for option in options:
                require(MONEY.fullmatch(option["precio_extra"]) is not None, "precio_extra inválido")


def validate_operational_status() -> None:
    envelope = load("operational_status.json")
    require(envelope.get("error") is None, "operational_status.error debe ser null")
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


if __name__ == "__main__":
    validate_catalog()
    validate_operational_status()
    print("Fixtures VAI-5 válidos.")
