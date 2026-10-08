package com.example.engine

object CadQueryArchitectureDoc {
    const val DOCUMENTATION_TITLE = "Análisis Técnico de CadQuery en Android"

    val REPORT_SUMMARY = """
# VIABILIDAD TÉCNICA: CadQuery & OpenCASCADE en Android

### 1. Diagnóstico de dependencias nativas
CadQuery se fundamenta en la biblioteca OCP (OpenCASCADE Python), la cual a su vez enlaza directamente con Open CASCADE Technology (OCCT). OCCT es un núcleo de modelado geométrico BRep industrial escrito en C++ compuesto por más de 50 bibliotecas dinámicas compartidas (libTKernel.so, libTKBRep.so, libTKTopAlgo.so, libTKFillet.so, etc.) con un tamaño compilado de más de 250 MB.

### 2. Limitaciones fundamentales en Android nativo (Bionic vs. glibc)
1. **Librería C del sistema**: Los paquetes precompilados de OCP y OpenCASCADE en PyPI y Conda están enlazados contra **glibc** (GNU C Library). Android utiliza la biblioteca C **Bionic**, que carece de soporte para múltiples llamadas a símbolos POSIX complejos y esquemas de almacenamiento de hilos (TLS) requeridos por el kernel de OCCT.
2. **Entornos móviles embebidos**: Motores como Chaquopy o QPython compilan módulos C usando el NDK de Android, pero OCP aún no dispone de 'wheels' nativos para Bionic/Android en PyPI.
3. **Alternativas viables en el dispositivo**:
   - **Termux + PRoot**: Es posible ejecutar CadQuery real en el propio dispositivo Android instalando un contenedor Debian/Arch ARM64 con glibc (`proot-distro install debian`) y conda/mamba (`conda install -c cadquery cadquery`). Este servidor se ejecuta en `http://127.0.0.1:5000` en segundo plano en el mismo teléfono.
   - **Servidor Remoto / Local Companion**: Ejecutar el script `cadquery_server.py` incluido en esta aplicación en tu ordenador personal (PC/Mac/Linux) o servidor en la misma red Wi-Fi.

### 3. Solución arquitectónica implementada en esta app
Para garantizar máxima transparencia, rigor técnico y usabilidad inmediata:
* **Modo Servidor CadQuery (OCP/Python Real)**: Envía tu código Python a un servidor CadQuery real (local en el dispositivo vía Termux, en red local o en la nube). Recibe la teselación geométrica real, sólidos BRep, registros de stdout/traceback y exportación STEP nativa de CadQuery.
* **Modo Autónomo Móvil (CadQuery Core Offline)**: Diseñado para permitir trabajar y modelar sin conexión a Internet ni servidor activo. Interpreta sintaxis y operaciones de CadQuery (Workplane, box, cylinder, sphere, fillet, chamfer, hole, cut, union, extrude, selectores '|Z', '>Y', show_object). Está **explícitamente identificado** en la interfaz para no presentarse falsamente como OCP local.
""".trimIndent()

    val PYTHON_SERVER_SCRIPT = """
#!/usr/bin/env python3
# ==============================================================================
# CADQUERY ENGINE SERVER - SERVIDOR DE EJECUCIÓN PARA ANDROID
# ==============================================================================
# Instrucciones de instalación:
#   pip install cadquery flask flask-cors
#
# Ejecución:
#   python cadquery_server.py
#
# Para ejecutar en el propio móvil mediante Termux:
#   pkg install proot-distro
#   proot-distro install debian
#   proot-distro login debian
#   apt update && apt install python3 python3-pip
#   conda install -c cadquery -c conda-forge cadquery
#   python cadquery_server.py
# ==============================================================================

import sys
import io
import time
import traceback
from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import cadquery as cq

app = Flask(__name__)
CORS(app)

latest_shapes = {}

def tessellate_shape(shape, name="Objeto", deflection=0.1):
    try:
        mesh = shape.val().tessellate(deflection)
        vertices = [[v.x, v.y, v.z] for v in mesh[0]]
        triangles = mesh[1] # list of [i1, i2, i3]
        
        # Bounding box y volumen
        bb = shape.val().BoundingBox()
        vol = shape.val().Volume()
        
        return {
            "name": name,
            "vertices": vertices,
            "triangles": triangles,
            "volume": float(vol),
            "bbox": {
                "minX": float(bb.xmin), "maxX": float(bb.xmax),
                "minY": float(bb.ymin), "maxY": float(bb.ymax),
                "minZ": float(bb.zmin), "maxZ": float(bb.zmax)
            }
        }
    except Exception as e:
        print(f"Error teselando {name}: {e}")
        return None

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "online",
        "engine": "CadQuery Real (OCP)",
        "version": getattr(cq, '__version__', '2.x'),
        "python": sys.version
    })

@app.route('/execute', methods=['POST'])
def execute_script():
    data = request.json or {}
    code = data.get("code", "")
    
    start_time = time.time()
    captured_objects = {}
    stdout_buf = io.StringIO()
    
    def custom_show_object(obj, name=None, options=None):
        nonlocal captured_objects
        obj_name = name or f"Pieza_{len(captured_objects)+1}"
        captured_objects[obj_name] = obj

    safe_globals = {
        "cq": cq,
        "cadquery": cq,
        "show_object": custom_show_object,
        "print": lambda *args: print(*args, file=stdout_buf)
    }

    try:
        exec(code, safe_globals)
        
        # Si no se usó show_object, buscar variables que sean Workplane o Shape
        if not captured_objects:
            for var_name, var_val in safe_globals.items():
                if isinstance(var_val, (cq.Workplane, cq.Shape)):
                    captured_objects[var_name] = var_val
                    
        rendered_objects = []
        global latest_shapes
        latest_shapes = captured_objects
        
        for name, shape in captured_objects.items():
            tess = tessellate_shape(shape, name=name)
            if tess:
                rendered_objects.append(tess)

        elapsed = int((time.time() - start_time) * 1000)
        return jsonify({
            "success": True,
            "objects": rendered_objects,
            "stdout": stdout_buf.getvalue(),
            "execution_time_ms": elapsed,
            "engine": "CadQuery OCP Real"
        })
    except Exception as exc:
        elapsed = int((time.time() - start_time) * 1000)
        tb_lines = traceback.format_exc().splitlines()
        
        # Detectar número de línea
        line_num = None
        for frame in traceback.extract_tb(sys.exc_info()[2]):
            if frame.filename == "<string>":
                line_num = frame.lineno
                
        return jsonify({
            "success": False,
            "error": str(exc),
            "line": line_num,
            "traceback": "\n".join(tb_lines),
            "stdout": stdout_buf.getvalue(),
            "execution_time_ms": elapsed
        }), 400

@app.route('/export/<fmt>', methods=['POST'])
def export_geometry(fmt):
    global latest_shapes
    if not latest_shapes:
        return jsonify({"error": "No hay geometría generada para exportar"}), 400
        
    shape_to_export = next(iter(latest_shapes.values()))
    out_file = f"/tmp/export_cadquery.{fmt.lower()}"
    
    fmt_upper = fmt.upper()
    if fmt_upper == 'STEP':
        cq.exporters.export(shape_to_export, out_file, cq.exporters.ExportTypes.STEP)
    elif fmt_upper == 'STL':
        cq.exporters.export(shape_to_export, out_file, cq.exporters.ExportTypes.STL)
    elif fmt_upper == 'OBJ':
        cq.exporters.export(shape_to_export, out_file, cq.exporters.ExportTypes.AMF)
    else:
        return jsonify({"error": f"Formato {fmt} no soportado"}), 400
        
    return send_file(out_file, as_attachment=True)

if __name__ == '__main__':
    print("Iniciando Servidor CadQuery para Android en http://0.0.0.0:5000 ...")
    app.run(host='0.0.0.0', port=5000, debug=False)
""".trimIndent()
}
