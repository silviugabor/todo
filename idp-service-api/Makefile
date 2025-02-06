.PHONY: build docker deploy restart verify-deployment check-health all stop

build:
	../gradlew :idp-service-api:clean :idp-service-api:build

docker:
	docker build -t silviugabor95/idp-service-api:latest .

deploy:
	kubectl apply -f idp-service-api.yaml

verify-deployment:
	@echo "Waiting for deployment to be ready..."
	kubectl wait --for=condition=available --timeout=300s deployment/idp-service-api-deployment
	@echo "Checking deployment status..."
	kubectl get deployment idp-service-api-deployment

check-health:
	@echo "Checking application health..."
	@POD_NAME=$$(kubectl get pods -l app=idp-service-api -o jsonpath="{.items[0].metadata.name}") && \
	kubectl exec $$POD_NAME -- wget -qO- http://localhost:8081/actuator/health | grep -q "UP" && \
	echo "✅ Application is healthy" || echo "❌ Application health check failed"

stop:
	@echo "Deleting Kubernetes resources..."
	kubectl delete -f idp-service-api.yaml

all: build docker deploy verify-deployment check-health

.SILENT: verify-deployment check-health