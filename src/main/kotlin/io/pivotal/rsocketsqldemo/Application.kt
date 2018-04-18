package io.pivotal.rsocketsqldemo

import com.nebhale.r2dbc.postgresql.PostgresqlConnectionFactory
import com.nebhale.r2dbc.core.R2dbc
import com.nebhale.r2dbc.postgresql.PostgresqlConnectionConfiguration
import reactor.core.publisher.toMono
import java.time.Duration


fun main(args: Array<String>) {
	val server = PostgresqlServer()

    server.start()

	val configuration = PostgresqlConnectionConfiguration.builder()
			.host(server.host)
			.port(server.port)
			.database(server.database)
			.username(server.username)
			.password(server.password)
			.build()

	try {
		val r2dbc = R2dbc(PostgresqlConnectionFactory(configuration))

		r2dbc.useHandle { it.execute("CREATE TABLE test ( value INTEGER )") }.block()

		r2dbc.inTransaction { it.createBatch().apply { for (i in 1..100) { this.add("INSERT INTO test VALUES ($i)") }  }.mapResult { it.toMono()} }
				.thenMany(r2dbc.inTransaction {
					it.select("SELECT value FROM test")
							.mapResult { result -> result.map({ row, _ -> row.get("value", Integer::class.java) }) }
				})
				.delayElements(Duration.ofMillis(100))
				.doOnNext(::println)
				.doOnError(::println)
				.blockLast()
	} finally {
		server.stop()
	}
}
