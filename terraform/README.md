# terraform/ — provisión local

Levanta el ambiente local con Terraform en vez de `docker build`/`docker run`
a mano. La imagen se construye con `docker build` real vía `local-exec`
(el `build {}` nativo del provider `kreuzwerker/docker` tiene un bug
conocido en Windows que corrompe el contexto — ver comentario en `main.tf`);
Terraform gestiona el ciclo de vida del contenedor.

## Requisitos

- Docker Desktop corriendo.
- Terraform >= 1.5.

## Uso

```
cd terraform
terraform init
terraform apply
```

Expone la app en `http://localhost:8080` (configurable con `-var host_port=...`).

```
terraform destroy
```

## Variables

| Variable            | Default                              | Descripción                          |
|---------------------|---------------------------------------|---------------------------------------|
| `image_tag`         | `local`                              | Tag de la imagen Docker construida.   |
| `host_port`         | `8080`                               | Puerto del host mapeado al contenedor.|
| `petstore_base_url` | `https://petstore.swagger.io/v2`     | Base URL de la API externa.           |
