.PHONY: build docker deploy restart verify-deployment check-health all stop

build:
	../gradlew :todo-api:clean :todo-api:build

docker:
	docker build -t silviugabor95/todo-api:latest .

deploy:
	kubectl apply -f todo-api.yaml

verify-deployment:
	@echo "Waiting for deployment to be ready..."
	kubectl wait --for=condition=available --timeout=300s deployment/todo-api-deployment
	@echo "Checking deployment status..."
	kubectl get deployment todo-api-deployment

check-health:
	@echo "Checking application health..."
	@POD_NAME=$$(kubectl get pods -l app=todo-api -o jsonpath="{.items[0].metadata.name}") && \
	kubectl exec $$POD_NAME -- wget -qO- http://localhost:8080/actuator/health | grep -q "UP" && \
	echo "✅ Application is healthy" || echo "❌ Application health check failed"

stop:
	@echo "Deleting Kubernetes resources..."
	kubectl delete -f todo-api.yaml

all: build docker deploy verify-deployment check-health

.SILENT: verify-deployment check-health