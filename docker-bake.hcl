group "default" {
  targets = ["account", "cash", "transfer", "front", "gateway", "notification", "liquibase"]
}

target "account" {

  context = "./my-bank-account-app"
  dockerfile = "Dockerfile"
  tags = ["my-bank-account:1.1.5"]
}

target "cash" {
  context = "./my-bank-cash-app"
  dockerfile = "Dockerfile"
  tags = ["my-bank-cash:1.1.0"]
}

target "transfer" {
  context = "./my-bank-transfer-app"
  dockerfile = "Dockerfile"
  tags = ["my-bank-transfer:1.1.0"]
}

target "front" {
  context = "./my-bank-front-app"
  dockerfile = "Dockerfile"
  tags = ["my-bank-front:1.0.1"]
}

target "gateway" {
  context = "./my-bank-gateway"
  dockerfile = "Dockerfile"
  tags = ["my-bank-gateway:1.0.1"]
}

target "notification" {
  context = "./my-bank-notification-app"
  dockerfile = "Dockerfile"
  tags = ["my-bank-notification:1.1.6"]
}

target "liquibase" {
  context = "./my-bank-liquibase"
  dockerfile = "Dockerfile"
  tags = ["my-bank-liquibase:1.0.2"]
}