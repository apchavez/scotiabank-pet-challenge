resource "null_resource" "build_image" {
  triggers = {
    dockerfile_hash = filesha256("${path.module}/../Dockerfile")
    build_gradle    = filesha256("${path.module}/../build.gradle")
    src_hash        = sha256(join("", [for f in sort(fileset("${path.module}/../src", "**")) : filesha256("${path.module}/../src/${f}")]))
  }

  provisioner "local-exec" {
    command     = "docker build -t pet-challenge:${var.image_tag} ."
    working_dir = "${path.module}/.."
  }
}

resource "docker_network" "pet_challenge" {
  name = "pet-challenge-net"
}

resource "docker_image" "redis" {
  name = "redis:7-alpine"
}

resource "docker_container" "redis" {
  name  = "pet-challenge-redis"
  image = docker_image.redis.image_id

  networks_advanced {
    name = docker_network.pet_challenge.name
  }
}

resource "docker_container" "pet_challenge" {
  name  = "pet-challenge"
  image = "pet-challenge:${var.image_tag}"

  env = [
    "PETSTORE_BASE_URL=${var.petstore_base_url}",
    "SPRING_DATA_REDIS_HOST=${docker_container.redis.name}",
    "SPRING_DATA_REDIS_PORT=6379",
  ]

  networks_advanced {
    name = docker_network.pet_challenge.name
  }

  ports {
    internal = 8080
    external = var.host_port
  }

  healthcheck {
    test         = ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
    interval     = "10s"
    timeout      = "3s"
    retries      = 5
    start_period = "15s"
  }

  depends_on = [null_resource.build_image, docker_container.redis]
}
