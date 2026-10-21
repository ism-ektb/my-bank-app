group "default" {
  targets = ["account", "cash", "transfer", "front", "gateway", "notification", "liquibase"]
}

variable "IMAGE_PREFIX" {
  default = "ghcr.io/ism-ektb"
}

target "account" {

  context = "./my-bank-account-app"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-account:1.3.1"]
}

target "cash" {
  context = "./my-bank-cash-app"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-cash:1.3.1"]
}

target "transfer" {
  context = "./my-bank-transfer-app"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-transfer:1.3.1"]
}

target "front" {
  context = "./my-bank-front-app"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-front:1.3.1"]
}

target "gateway" {
  context = "./my-bank-gateway"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-gateway:1.3.1"]
}

target "notification" {
  context = "./my-bank-notification-app"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-notification:1.3.1"]
}

target "liquibase" {
  context = "./my-bank-liquibase"
  dockerfile = "Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-liquibase:1.0.2"]
}