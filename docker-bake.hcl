group "default" {
  targets = ["account", "cash", "transfer", "front", "gateway", "notification", "liquibase"]
}

variable "IMAGE_PREFIX" {
  default = "ghcr.io/ism-ektb"
}

target "account" {

  context = "."
  dockerfile = "./my-bank-account-app/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-account:1.3.1"]
}

target "cash" {
  context = "."
  dockerfile = "./my-bank-cash-app/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-cash:1.3.1"]
}

target "transfer" {
  context = "."
  dockerfile = "./my-bank-transfer-app/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-transfer:1.3.1"]
}

target "front" {
  context = "."
  dockerfile = "./my-bank-front-app/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-front:1.3.2"]
}

target "gateway" {
  context = "."
  dockerfile = "./my-bank-gateway/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-gateway:1.3.1"]
}

target "notification" {
  context = "."
  dockerfile = "./my-bank-notification-app/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-notification:1.3.1"]
}

target "liquibase" {
  context = "."
  dockerfile = "./my-bank-liquibase/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-liquibase:1.0.2"]
}