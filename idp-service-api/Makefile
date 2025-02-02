.PHONY: build docker deploy restart all

build:
	../gradlew :idp-service-api:clean :idp-service-api:build

docker:
	docker build -t silviugabor95/idp-service-api:latest .

deploy:
	kubectl apply -f idp-service-api.yaml

restart:
	kubectl rollout restart deployment idp-service-api-deployment

stop:
	@echo "Deleting Kubernetes resources..."
	kubectl delete -f idp-service-api.yaml

all: build docker deploy restart