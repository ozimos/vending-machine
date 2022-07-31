clean:
	rm -rf target

run:
	clj -M:dev:cider

repl:
	clj -M:dev:nrepl

test:
	clj -M:test

uberjar:
	clj -T:build all
