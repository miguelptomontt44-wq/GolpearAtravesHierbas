# AtravesarPlantas

Plugin para **Paper 26.x** que te deja golpear mobs a traves de pasto, flores,
helechos, pasto alto y cualquier planta (de 1 o 2 bloques de alto) que en vanilla
te bloquea el golpe.

## Como obtener el .jar (sin instalar nada)

1. Crea un repositorio en GitHub y sube todos los archivos de esta carpeta
   (incluida la carpeta oculta `.github`).
2. Ve a la pestana **Actions**. El workflow **Build** se ejecuta solo al subir.
3. Cuando termine (check verde), entra a la ejecucion y descarga el artifact
   **AtravesarPlantas** (es un zip con el `.jar` dentro).

### Alternativa: Releases
Crea un tag que empiece con `v` (por ejemplo `v1.0.0`) y el .jar aparecera
automaticamente en la seccion **Releases** del repositorio.

## Instalacion
Copia el `.jar` a la carpeta `plugins/` de tu servidor Paper y reinicia.

## Configuracion (`plugins/AtravesarPlantas/config.yml`)
| Opcion | Default | Descripcion |
|---|---|---|
| `reach` | `3.0` | Alcance maximo del golpe |
| `ray-size` | `0.0` | Margen extra del hitbox |
| `protect-plant` | `true` | No romper la planta al golpear al mob |
| `allow-players` | `true` | Permite golpear jugadores tambien |

Comando: `/atravesarplantas reload` (permiso `atravesarplantas.admin`).
Permiso para usar la funcion: `atravesarplantas.use` (por defecto todos).

## Compatibilidad / version de la API
El `pom.xml` compila contra `paper.api.version` (por defecto `26.1.2.build.67-stable`)
y el plugin usa solo API estable, asi que deberia cargar en 26.2 y 26.3.
Si quieres compilar contra una version especifica, cambia esa propiedad en el
`pom.xml` por la que aparezca en https://repo.papermc.io/ para `io.papermc.paper:paper-api`.
Requiere **Java 25**.
