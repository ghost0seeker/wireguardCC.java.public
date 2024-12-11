#!/usr/bin/env fish

if not test -d containers/app
	mkdir -p containers/app
end

sudo chown -R $USER:$USER containers/app

rsync -a --info=progress2 --exclude-from=.rsyncignore . containers/app
