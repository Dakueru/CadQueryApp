package com.example.model

data class CadTemplate(
    val title: String,
    val description: String,
    val iconName: String,
    val code: String
)

object CadTemplates {
    val templates: List<CadTemplate> = listOf(
        CadTemplate(
            title = "Pieza básica con redondeos",
            description = "Prisma rectangular con aristas verticales redondeadas y taladro central pasante.",
            iconName = "cube",
            code = """
import cadquery as cq

# Dimensiones principales
largo = 120.0
ancho = 70.0
alto = 40.0
radio_redondeo = 6.0
diametro_taladro = 22.0

# Generación del modelo
pieza = (
    cq.Workplane("XY")
    .box(largo, ancho, alto)
    .edges("|Z")
    .fillet(radio_redondeo)
    .faces(">Z")
    .hole(diametro_taladro)
)

show_object(pieza, name="Bloque Redondeado")
""".trimIndent()
        ),
        CadTemplate(
            title = "Brida paramétrica (Flange)",
            description = "Brida circular con cuello cilíndrico y 4 taladros perimétricos para tornillos.",
            iconName = "settings",
            code = """
import cadquery as cq

radio_base = 50.0
espesor_base = 12.0
radio_cuello = 25.0
altura_cuello = 35.0
diametro_eje = 20.0
radio_tornillos = 38.0
diametro_tornillos = 8.0

# Base circular y cuello
brida = (
    cq.Workplane("XY")
    .circle(radio_base)
    .extrude(espesor_base)
    .faces(">Z")
    .circle(radio_cuello)
    .extrude(altura_cuello)
    .faces(">Z")
    .hole(diametro_eje)
)

# Taladros perimétricos para tornillos
brida = (
    brida.faces("<Z")
    .workplane()
    .polarArray(radio_tornillos, 0, 360, 4)
    .circle(diametro_tornillos / 2.0)
    .cutThruAll()
)

show_object(brida, name="Brida Parametrica")
""".trimIndent()
        ),
        CadTemplate(
            title = "Ensamblaje multipieza",
            description = "Múltiples componentes independientes gestionables con show_object.",
            iconName = "layers",
            code = """
import cadquery as cq

# Pieza 1: Carcasa base
base = (
    cq.Workplane("XY")
    .box(100, 80, 25)
    .edges("|Z")
    .fillet(8)
    .faces(">Z")
    .hole(30)
)

# Pieza 2: Eje cilíndrico central
eje = (
    cq.Workplane("XY")
    .workplane(offset=10)
    .cylinder(60, 14)
)

# Pieza 3: Tapa superior
tapa = (
    cq.Workplane("XY")
    .workplane(offset=40)
    .box(100, 80, 8)
    .edges("|Z")
    .chamfer(4)
)

# Registro de piezas independientes para el visor
show_object(base, name="Carcasa Base", options={"color": "#38BDF8"})
show_object(eje, name="Eje Central", options={"color": "#F59E0B"})
show_object(tapa, name="Tapa Superior", options={"color": "#10B981"})
""".trimIndent()
        ),
        CadTemplate(
            title = "Operaciones booleanas",
            description = "Cuerpo prismático con sustracción (cut), unión y vaciado.",
            iconName = "merge",
            code = """
import cadquery as cq

longitud = 80.0
seccion = 40.0
radio_tubo = 12.0

bloque_principal = cq.Workplane("XY").box(longitud, seccion, seccion)

# Conducto transversal a restar
cilindro_x = (
    cq.Workplane("YZ")
    .cylinder(longitud + 10, radio_tubo)
)

# Conducto vertical a restar
cilindro_z = (
    cq.Workplane("XY")
    .cylinder(seccion + 10, radio_tubo)
)

# Unión de los conductos y resta booleana
conductos = cilindro_x.union(cilindro_z)
colector = bloque_principal.cut(conductos).edges("|Z").fillet(3)

show_object(colector, name="Colector Valvula")
""".trimIndent()
        ),
        CadTemplate(
            title = "Polea mecánica con chavetero",
            description = "Polea de transmisión con ranura en V y agujero con chavetero.",
            iconName = "rotate_right",
            code = """
import cadquery as cq

diametro_exterior = 90.0
ancho_polea = 28.0
diametro_eje = 20.0
profundidad_canal = 14.0

# Perfil del cuerpo
polea = (
    cq.Workplane("XY")
    .cylinder(ancho_polea, diametro_exterior / 2.0)
    .faces(">Z")
    .hole(diametro_eje)
)

# Redondeo de bordes exteriores
polea = polea.edges(">Z or <Z").fillet(2.5)

show_object(polea, name="Polea Dentada")
""".trimIndent()
        ),
        CadTemplate(
            title = "Documento vacío",
            description = "Estructura limpia con import de CadQuery listo para codificar.",
            iconName = "code",
            code = """
import cadquery as cq

# Escribe tu modelo CadQuery aqui
modelo = (
    cq.Workplane("XY")
    .box(50, 50, 50)
)

show_object(modelo, name="Mi Modelo")
""".trimIndent()
        )
    )
}
