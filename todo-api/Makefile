.PHONY: build docker deploy restart all

build:
	../gradlew :todo-api:clean :todo-api:build

docker:
	docker build -t silviugabor95/todo-api:latest .

deploy:
	kubectl apply -f todo-api.yaml

restart:
	kubectl rollout restart deployment todo-api-deployment

stop:
	@echo "Deleting Kubernetes resources..."
	kubectl delete -f todo-api.yaml

all: build docker deploy restart