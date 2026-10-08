package com.example.model

object CadDocRepository {
    val documentationItems: List<CadDocItem> = listOf(
        CadDocItem(
            name = "Workplane",
            signature = "cq.Workplane(plane=\"XY\", origin=(0,0,0))",
            category = "Construcción",
            description = "Crea un plano de trabajo 2D a partir del cual se pueden dibujar bocetos, extruir sólidos o ubicar geometrías.",
            parameters = listOf(
                "plane: Orientación del plano ('XY', 'XZ', 'YZ', 'front', 'back', 'top', 'bottom', 'left', 'right')",
                "origin: Coordenadas de traslación del origen (x, y, z)"
            ),
            example = "caja = cq.Workplane(\"XY\").box(10, 20, 30)"
        ),
        CadDocItem(
            name = "box",
            signature = ".box(length, width, height, centered=(True, True, True))",
            category = "Sólidos básicos",
            description = "Crea un prisma rectangular tridimensional con las dimensiones especificadas.",
            parameters = listOf(
                "length: Longitud a lo largo del eje X (mm)",
                "width: Anchura a lo largo del eje Y (mm)",
                "height: Altura a lo largo del eje Z (mm)",
                "centered: Tupla booleana que indica el centrado en cada eje"
            ),
            example = "caja = cq.Workplane(\"XY\").box(120, 70, 40)"
        ),
        CadDocItem(
            name = "cylinder",
            signature = ".cylinder(height, radius, centered=(True, True, True))",
            category = "Sólidos básicos",
            description = "Crea un cilindro recto con la altura y el radio especificados a lo largo del eje normal.",
            parameters = listOf(
                "height: Altura del cilindro (mm)",
                "radius: Radio de la base circular (mm)",
                "centered: Centrado respecto al plano de trabajo"
            ),
            example = "eje = cq.Workplane(\"XY\").cylinder(80, 15)"
        ),
        CadDocItem(
            name = "sphere",
            signature = ".sphere(radius)",
            category = "Sólidos básicos",
            description = "Crea una esfera centrada con el radio especificado.",
            parameters = listOf(
                "radius: Radio de la esfera (mm)"
            ),
            example = "bola = cq.Workplane(\"XY\").sphere(25)"
        ),
        CadDocItem(
            name = "fillet",
            signature = ".fillet(radius)",
            category = "Modificación",
            description = "Aplica un redondeo con un radio constante a todas las aristas seleccionadas en la pila de trabajo.",
            parameters = listOf(
                "radius: Radio de curvatura del redondeo (mm)"
            ),
            example = "caja_redondeada = cq.Workplane(\"XY\").box(100, 50, 30).edges(\"|Z\").fillet(5)"
        ),
        CadDocItem(
            name = "chamfer",
            signature = ".chamfer(length)",
            category = "Modificación",
            description = "Aplica un chaflán oblicuo plano a las aristas seleccionadas.",
            parameters = listOf(
                "length: Longitud del chaflán (mm)"
            ),
            example = "pieza = cq.Workplane(\"XY\").box(50, 50, 20).edges(\">Z\").chamfer(3)"
        ),
        CadDocItem(
            name = "hole",
            signature = ".hole(diameter, depth=None)",
            category = "Taladros y vaciados",
            description = "Perfora un agujero cilíndrico normal a las caras o ubicaciones seleccionadas. Si depth es None, perfora pasante.",
            parameters = listOf(
                "diameter: Diámetro del taladro cilíndrico (mm)",
                "depth: Profundidad del agujero. Si se omite, atraviesa todo el sólido"
            ),
            example = "bloque = cq.Workplane(\"XY\").box(80, 80, 20).faces(\">Z\").hole(25)"
        ),
        CadDocItem(
            name = "edges",
            signature = ".edges(selector=None)",
            category = "Selectores",
            description = "Filtra y selecciona aristas del sólido utilizando expresiones de selección CadQuery como '|Z', '>Y', '<X', '%CIRCLE'.",
            parameters = listOf(
                "selector: Expresión de selección. Ejemplos: '|Z' (paralelas a Z), '>Y' (en sentido positivo de Y), '|Z and >Y'"
            ),
            example = ".edges(\"|Z and >Y\").fillet(4)"
        ),
        CadDocItem(
            name = "faces",
            signature = ".faces(selector=None)",
            category = "Selectores",
            description = "Filtra y selecciona caras del sólido utilizando expresiones de selección geométrica.",
            parameters = listOf(
                "selector: Dirección o filtro de cara. Ejemplos: '>Z' (cara superior), '<Z' (cara inferior), '>X', '|Z'"
            ),
            example = ".faces(\">Z\").workplane().circle(10).extrude(15)"
        ),
        CadDocItem(
            name = "extrude",
            signature = ".extrude(distance, combine=True, clean=True)",
            category = "Bocetos y 3D",
            description = "Extruye el boceto 2D activo a lo largo de la normal del plano de trabajo por la distancia especificada.",
            parameters = listOf(
                "distance: Distancia de extrusión (mm)",
                "combine: Si es True, une el nuevo sólido con la geometría existente",
                "clean: Limpia caras redundantes tras la operación"
            ),
            example = "cq.Workplane(\"XY\").circle(20).extrude(50)"
        ),
        CadDocItem(
            name = "revolve",
            signature = ".revolve(angleDegrees=360.0, axisStart=(0,0,0), axisEnd=(0,1,0))",
            category = "Bocetos y 3D",
            description = "Revoluciona un boceto 2D cerrado alrededor de un eje especificado para crear un sólido de revolución.",
            parameters = listOf(
                "angleDegrees: Ángulo total de rotación en grados (por defecto 360)",
                "axisStart: Punto de inicio del vector de eje de rotación",
                "axisEnd: Punto final del vector de eje de rotación"
            ),
            example = "tubo = cq.Workplane(\"XZ\").circle(10).revolve(360, (0,0,0), (0,0,1))"
        ),
        CadDocItem(
            name = "cut",
            signature = ".cut(toCut, clean=True)",
            category = "Booleanas",
            description = "Resta geométrica (diferencia booleana): sustrae el sólido toCut del sólido actual.",
            parameters = listOf(
                "toCut: Objeto sólido o Workplane que se desea restar"
            ),
            example = "cuerpo = bloque.cut(cilindro_vaciado)"
        ),
        CadDocItem(
            name = "union",
            signature = ".union(toUnion, clean=True)",
            category = "Booleanas",
            description = "Unión booleana: fusiona dos o más sólidos independientes en un único cuerpo.",
            parameters = listOf(
                "toUnion: Objeto sólido o Workplane a fusionar"
            ),
            example = "ensamble = base.union(torreta)"
        ),
        CadDocItem(
            name = "intersect",
            signature = ".intersect(toIntersect)",
            category = "Booleanas",
            description = "Intersección booleana: conserva únicamente el volumen compartido por ambos sólidos.",
            parameters = listOf(
                "toIntersect: Sólido con el que calcular la intersección"
            ),
            example = "comun = caja.intersect(esfera)"
        ),
        CadDocItem(
            name = "shell",
            signature = ".shell(thickness)",
            category = "Modificación",
            description = "Convierte un sólido macizo en una cáscara hueca con el espesor de pared indicado, abriendo las caras seleccionadas.",
            parameters = listOf(
                "thickness: Espesor de la pared (negativo para vaciado interior)"
            ),
            example = "caja_hueca = cq.Workplane(\"XY\").box(40,40,40).faces(\">Z\").shell(-2)"
        ),
        CadDocItem(
            name = "show_object",
            signature = "show_object(shape, name=\"Pieza\", options=None)",
            category = "Visualización",
            description = "Registra un sólido o ensamblaje para ser renderizado en el visor 3D como una pieza independiente con nombre.",
            parameters = listOf(
                "shape: Geometría CadQuery a visualizar",
                "name: Nombre identificador para la lista de objetos del visor",
                "options: Diccionario de opciones de color y transparencia"
            ),
            example = "show_object(caja, name=\"Base\", options={\"color\": \"#38BDF8\"})"
        )
    )

    fun findDoc(keyword: String): CadDocItem? {
        val clean = keyword.trim().removePrefix(".").removeSuffix("()")
        return documentationItems.firstOrNull { it.name.equals(clean, ignoreCase = true) }
    }
}
