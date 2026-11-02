"""
Reduce el tamano del GeoJSON mundial de paises (Natural Earth 10m, ~14MB) a un
tamano razonable para servir en el frontend: simplificacion Douglas-Peucker por
anillo + redondeo de coordenadas + solo las propiedades necesarias (nombre, ISO2).
Uso: python simplificar_geojson.py <entrada.geojson> <salida.geojson> <tolerancia_grados>
"""
import json
import sys


def distancia_punto_segmento(p, a, b):
    (px, py), (ax, ay), (bx, by) = p, a, b
    dx, dy = bx - ax, by - ay
    if dx == 0 and dy == 0:
        return ((px - ax) ** 2 + (py - ay) ** 2) ** 0.5
    t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
    t = max(0.0, min(1.0, t))
    cx, cy = ax + t * dx, ay + t * dy
    return ((px - cx) ** 2 + (py - cy) ** 2) ** 0.5


def douglas_peucker(puntos, tolerancia):
    if len(puntos) < 3:
        return puntos
    pila = [(0, len(puntos) - 1)]
    mantener = [False] * len(puntos)
    mantener[0] = mantener[-1] = True
    while pila:
        inicio, fin = pila.pop()
        if fin - inicio < 2:
            continue
        max_dist, idx_max = -1, -1
        for i in range(inicio + 1, fin):
            d = distancia_punto_segmento(puntos[i], puntos[inicio], puntos[fin])
            if d > max_dist:
                max_dist, idx_max = d, i
        if max_dist > tolerancia:
            mantener[idx_max] = True
            pila.append((inicio, idx_max))
            pila.append((idx_max, fin))
    return [p for p, k in zip(puntos, mantener) if k]


def redondear(puntos, decimales=4):
    return [[round(x, decimales), round(y, decimales)] for x, y in puntos]


def simplificar_anillo(anillo, tolerancia):
    reducido = douglas_peucker(anillo, tolerancia)
    if len(reducido) < 4:
        return anillo
    return redondear(reducido)


def simplificar_geometria(geom, tolerancia):
    tipo = geom["type"]
    if tipo == "Polygon":
        geom["coordinates"] = [simplificar_anillo(anillo, tolerancia) for anillo in geom["coordinates"]]
    elif tipo == "MultiPolygon":
        geom["coordinates"] = [
            [simplificar_anillo(anillo, tolerancia) for anillo in poligono]
            for poligono in geom["coordinates"]
        ]
    return geom


def main():
    entrada, salida, tolerancia = sys.argv[1], sys.argv[2], float(sys.argv[3])
    with open(entrada, encoding="utf-8") as f:
        datos = json.load(f)

    features = []
    for feature in datos["features"]:
        props = feature["properties"]
        iso2 = props.get("ISO3166-1-Alpha-2")
        if not iso2 or iso2 == "-99":
            continue
        features.append({
            "type": "Feature",
            "properties": {"nombre": props.get("name"), "iso2": iso2.lower()},
            "geometry": simplificar_geometria(feature["geometry"], tolerancia),
        })

    salida_json = {"type": "FeatureCollection", "features": features}
    with open(salida, "w", encoding="utf-8") as f:
        json.dump(salida_json, f, separators=(",", ":"), ensure_ascii=False)

    print(f"features: {len(features)}")


if __name__ == "__main__":
    main()
