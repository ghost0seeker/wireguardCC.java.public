#!/usr/bin/env fish

if not test -d app
	mkdir -p app
end

sudo chown -R $USER:$USER app/

rsync -a --info=progress2 --exclude-from=.rsyncignore . containers/app
