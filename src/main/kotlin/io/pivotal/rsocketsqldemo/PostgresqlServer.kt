/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.rsocketsqldemo

import java.nio.file.Paths

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.cachedRuntimeConfig
import java.util.*

class PostgresqlServer(
		val database: String = randomAlphanumeric(8),
		val host: String = "localhost",
		val port: Int = 1345,
		val password: String = randomAlphanumeric(16),
		val username: String = randomAlphanumeric(16)) {

	private val logger = LoggerFactory.getLogger("test.postgresql-server")

	private val server = EmbeddedPostgres(Paths.get(System.getProperty("java.io.tmpdir"), "pgembed", "data", database).toString())

	private var dataSource: HikariDataSource? = null

	private val cachePath = Paths.get(System.getProperty("java.io.tmpdir"), "pgembed")


	fun start() {
		this.logger.info("PostgreSQL server starting")
		this.server.start(cachedRuntimeConfig(cachePath), host, port, this.database, this.username, this.password, emptyList())

		this.dataSource = this.server.connectionUrl
				.map({ url -> HikariDataSource(HikariConfig(Properties().apply { put("jdbcUrl", url) })) })
				.orElseThrow({ IllegalStateException("Unable to determine JDBC URI") })

		this.logger.info("PostgreSQL server started")
	}

	fun stop() {
		this.logger.info("PostgreSQL server stopping")
		this.dataSource!!.close()
		this.server.stop()
		this.logger.info("PostgreSQL server stopped")
	}

}
