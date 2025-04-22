gcloud auth login

docker tag com.food.ordering.system/order.service:$1 asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/order.service:$1
docker tag com.food.ordering.system/payment.service:$1 asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/payment.service:$1
docker tag com.food.ordering.system/customer.service:$1 asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/customer.service:$1
docker tag com.food.ordering.system/restaurant.service:$1 asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/restaurant.service:$1


docker push asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/order.service:$1
docker push asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/payment.service:$1
docker push asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/customer.service:$1
docker push asia-northeast3-docker.pkg.dev/food-ordering-system-457305/food-ordering-system/restaurant.service:$1