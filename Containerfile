FROM docker.io/library/alpine:latest

# Install OpenJDK and required packages
RUN apk add --no-cache \
    fish \
    openjdk17 \
    #wireguard-tools \
    #iptables \
    iproute2 \
    net-tools \
    iputils \
    curl \
    maven

# Set working directory
WORKDIR /app

# Copy source code and pom.xml
COPY src/ src/
COPY pom.xml ./

# Build the application
#RUN mvn clean package

# Keep container running for testing
CMD ["tail", "-f", "/dev/null"]
