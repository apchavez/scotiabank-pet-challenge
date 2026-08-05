output "app_url" {
  description = "URL local de la app levantada por Terraform."
  value       = "http://localhost:${var.host_port}"
}

output "container_id" {
  value = docker_container.pet_challenge.id
}
