FROM docker.io/library/openjdk:17-slim

RUN apt-get update && apt-get install -y \
	maven \
	git \
	fish \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/*

RUN echo /usr/bin/fish | tee -a /etc/shells \
    && chsh -s /usr/bin/fish

WORKDIR /workbench

CMD ["fish"]
