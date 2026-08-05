variable "image_tag" {
  description = "Tag para la imagen Docker construida localmente."
  type        = string
  default     = "local"
}

variable "host_port" {
  description = "Puerto del host mapeado al 8080 del contenedor."
  type        = number
  default     = 8080
}

variable "petstore_base_url" {
  description = "Base URL de la API externa de Petstore."
  type        = string
  default     = "https://petstore.swagger.io/v2"
}
