
1. 먼저 cp-helm-charts 를 클론해옵니다.
```
git clone https://github.com/confluentinc/cp-helm-charts
```

2. 쿠버네티스 배포 툴일 helm 설치 ([helm 설치 가이드](https://helm.sh/docs/intro/install/))
```
brew install helm
```

3. 다음 커맨드 실행
```
helm install local-confluent-kafka helm/cp-helm-charts --version 0.6.0
```