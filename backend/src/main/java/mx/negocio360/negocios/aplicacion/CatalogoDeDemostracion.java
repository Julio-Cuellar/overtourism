package mx.negocio360.negocios.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Los doce negocios simulados del stand (D-105): cinco restaurantes, tres museos y cuatro misceláneas repartidos entre el
 * Centro, Analco, el Barrio del Artista y Cholula. Todo es inventado: nombres, descripciones, reseñas y respuestas. Los ids son
 * fijos para que la siembra sea idempotente. Los dos o tres autores de cada reseña son dispositivos ficticios.
 */
final class CatalogoDeDemostracion {

    /** La cuenta de demostracion que «pide» el alta de todos. No existe en Identidad: es solo un id. */
    static final UUID CUENTA = id(0xCC, 0);

    record Resena(int calificacion, String comentario, String respuesta, int hacenDias) {
    }

    record Negocio(int indice, String nombre, String giro, String descripcion, String calle, String numero,
                   String colonia, String municipio, String codigoPostal, String latitud, String longitud,
                   List<String> subcategorias, List<String> dieta, List<String> accesibilidad, int duracionMin,
                   int costoMxn, String abre, String cierra, boolean disponibilidad, List<Resena> resenas) {

        UUID id() {
            return CatalogoDeDemostracion.id(0xAA, indice);
        }

        UUID eventoId() {
            return CatalogoDeDemostracion.id(0xEE, indice);
        }

        BigDecimal lat() {
            return new BigDecimal(latitud);
        }

        BigDecimal lon() {
            return new BigDecimal(longitud);
        }

        /** El autor ficticio de la reseña numero {@code n} de este negocio. */
        UUID autor(int n) {
            return CatalogoDeDemostracion.id(0xD0 + n, indice);
        }
    }

    static final List<Negocio> NEGOCIOS = List.of(
            new Negocio(1, "Fonda Doña Chela (demo)", "RESTAURANTE", "Mole, chalupas y tamales de la casa. Negocio de demostración.",
                    "5 de Mayo", "312", "Centro", "Puebla", "72000", "19.043300", "-98.198200",
                    List.of("GASTRONOMIA_TIPICA", "COMIDA_CORRIDA"), List.of("VEGETARIANA"), List.of("ACCESO_SIN_ESCALONES"),
                    75, 180, "08:00", "18:00", true, List.of(
                            new Resena(5, "El mole poblano estaba buenísimo y la atención fue muy amable.",
                                    "¡Gracias por visitarnos, vuelve pronto!", 3),
                            new Resena(4, "Buena comida corrida, el comedor es chico pero cómodo.", null, 9),
                            new Resena(5, "Los tamales de rajas, de lo mejor del centro.", null, 20))),
            new Negocio(2, "Cocina de Analco Los Sapos (demo)", "RESTAURANTE", "Antojitos y cemitas junto al barrio de Analco.",
                    "4 Oriente", "418", "Analco", "Puebla", "72000", "19.041500", "-98.193800",
                    List.of("GASTRONOMIA_TIPICA", "MIXTO"), List.of("VEGANA", "SIN_LACTOSA"), List.of(),
                    60, 140, "09:00", "20:00", false, List.of(
                            new Resena(4, "Cemitas generosas y salsa casera.", "Qué gusto que te gustara.", 2),
                            new Resena(3, "Buen sabor pero tardaron en servir.", null, 6))),
            new Negocio(3, "Pasta y Pizza Barrio del Artista (demo)", "RESTAURANTE", "Cocina italiana a unos pasos de los talleres de artistas.",
                    "6 Norte", "602", "Barrio del Artista", "Puebla", "72000", "19.044800", "-98.194500",
                    List.of("EXTRANJERA", "VERANO"), List.of("VEGETARIANA", "SIN_GLUTEN"), List.of("BANO_ACCESIBLE"),
                    90, 260, "13:00", "23:00", true, List.of(
                            new Resena(5, "La pasta fresca es una maravilla y la terraza es muy agradable.",
                                    "Gracias, esperamos verte otra vez.", 1),
                            new Resena(2, "Mucha fila y el ruido no dejaba platicar.", null, 5),
                            new Resena(4, "Opciones sin gluten, se agradece.", null, 12))),
            new Negocio(4, "Cafetería La Plaza Cholula (demo)", "RESTAURANTE", "Café de olla y pan artesanal frente a la plaza.",
                    "Portal Guerrero", "9", "Centro", "San Pedro Cholula", "72760", "19.063800", "-98.306700",
                    List.of("MIXTO", "VERANO"), List.of("VEGETARIANA"), List.of("ACCESO_SIN_ESCALONES", "PERSONAL_LSM"),
                    45, 90, "08:00", "21:00", false, List.of(
                            new Resena(5, "Café de olla perfecto después de subir a la pirámide.", null, 4),
                            new Resena(4, "Pan calientito y buen precio.", "¡Gracias!", 11))),
            new Negocio(5, "Fonda Casa de Cholula (demo)", "RESTAURANTE", "Comida corrida y caldos como en casa.",
                    "3 Poniente", "104", "San Andrés", "San Andrés Cholula", "72810", "19.052000", "-98.299000",
                    List.of("COMIDA_CORRIDA", "GASTRONOMIA_TIPICA"), List.of(), List.of(),
                    60, 110, "12:00", "17:00", true, List.of(
                            new Resena(4, "Caldo de pollo y tortillas hechas a mano.", null, 7),
                            new Resena(3, "Rico pero se llena muy rápido.", null, 15))),
            new Negocio(6, "Museo del Talavera Viva (demo)", "MUSEO", "Historia de la talavera poblana, con taller de pintura.",
                    "5 Oriente", "304", "Centro", "Puebla", "72000", "19.043900", "-98.196400",
                    List.of("HISTORIA", "ARTE", "TALAVERA"), List.of(), List.of("ACCESO_SIN_ESCALONES", "BANO_ACCESIBLE"),
                    120, 80, "10:00", "17:00", true, List.of(
                            new Resena(5, "Muy bien explicado y el taller es una experiencia única.", "Gracias por tu visita.", 2),
                            new Resena(5, "Perfecto para ir con niños.", null, 8),
                            new Resena(4, "Falta más señalización pero vale la pena.", null, 14))),
            new Negocio(7, "Casa de la Historia Analco (demo)", "MUSEO", "Recorrido por la historia del barrio y sus tianguis.",
                    "Callejón de los Sapos", "11", "Analco", "Puebla", "72000", "19.042000", "-98.192900",
                    List.of("HISTORIA", "CULTURA"), List.of(), List.of("SENALIZACION_BRAILLE"),
                    90, 60, "10:00", "16:00", false, List.of(
                            new Resena(4, "Guía muy amable, buen recorrido corto.", null, 3),
                            new Resena(5, "Ideal para entender el centro histórico.", "¡Gracias!", 10))),
            new Negocio(8, "Galería Taller del Artista (demo)", "MUSEO", "Obra de pintores y escultores de Puebla, con entrada libre.",
                    "6 Norte", "416", "Barrio del Artista", "Puebla", "72000", "19.045200", "-98.195100",
                    List.of("ARTE", "CULTURA"), List.of(), List.of("ACCESO_SIN_ESCALONES"),
                    60, 0, "11:00", "19:00", true, List.of(
                            new Resena(5, "Pude ver a los artistas trabajando y conversar con ellos.", null, 1),
                            new Resena(4, "Entrada libre y obra de calidad.", null, 6))),
            new Negocio(9, "Dulcería La Tradición (demo)", "MISCELANEA", "Camotes, borrachitos y alfeñiques hechos en casa.",
                    "6 Oriente", "206", "Centro", "Puebla", "72000", "19.043100", "-98.195700",
                    List.of("DULCES_TIPICOS"), List.of(), List.of("ACCESO_SIN_ESCALONES"),
                    20, 120, "10:00", "20:00", true, List.of(
                            new Resena(5, "Los camotes de guayaba están deliciosos, ideales de regalo.", "Gracias, ¡buen viaje!", 2),
                            new Resena(4, "Variedad enorme y empaque bonito.", null, 9))),
            new Negocio(10, "Talavera El Taller (demo)", "MISCELANEA", "Loza y azulejo de talavera pintados a mano.",
                    "5 Oriente", "412", "Centro", "Puebla", "72000", "19.043700", "-98.195000",
                    List.of("TALAVERA", "ARTESANIAS"), List.of(), List.of(),
                    40, 450, "09:30", "18:30", false, List.of(
                            new Resena(5, "Piezas hermosas y nos explicaron cómo distinguir la talavera auténtica.", null, 5),
                            new Resena(3, "Bonito pero algo caro.", null, 13),
                            new Resena(4, "Envían a todo el país.", "Gracias por la reseña.", 18))),
            new Negocio(11, "Artesanías Mercado El Parián (demo)", "MISCELANEA", "Textiles, bordados y figuras de barro.",
                    "6 Norte", "2", "Centro", "Puebla", "72000", "19.045600", "-98.196800",
                    List.of("ARTESANIAS", "TEXTILES", "ANTIGUEDADES"), List.of(), List.of(),
                    50, 300, "10:00", "19:00", true, List.of(
                            new Resena(4, "Mucho para ver, hay que regatear con amabilidad.", null, 4),
                            new Resena(5, "Compré un rebozo precioso.", null, 12))),
            new Negocio(12, "Abarrotes y Bebidas Doña Lupe (demo)", "MISCELANEA", "La miscelánea de la esquina: bebidas, botanas y lo de siempre.",
                    "9 Poniente", "507", "Analco", "Puebla", "72000", "19.040800", "-98.200500",
                    List.of("ABARROTES", "PAPELERIA_REGALOS"), List.of(), List.of(),
                    10, 60, "07:00", "22:00", false, List.of(
                            new Resena(4, "Siempre tienen lo que necesito y atienden rápido.", null, 3),
                            new Resena(5, "Tienda de barrio con trato amable.", "¡Gracias, vecino!", 16))));

    private CatalogoDeDemostracion() {
    }

    /** Un UUID con forma de v7 y variante RFC 4122, fijo y facil de reconocer: {@code (prefijo, indice)}. */
    static UUID id(int prefijo, int indice) {
        return new UUID(0x0192_0000_0000_7000L | ((long) prefijo << 16) | indice, 0x8000_0000_0000_0000L | indice);
    }
}
