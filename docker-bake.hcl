group "default" {
  targets = ["account"]
}

variable "IMAGE_PREFIX" {
  default = "ghcr.io/ism-ektb"
}

target "account" {

  context = "."
  dockerfile = "./my-bank-account-app/Dockerfile"
  tags = ["${IMAGE_PREFIX}/my-bank-account:1.3.1"]
}