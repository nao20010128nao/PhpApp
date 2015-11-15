/**
 * Copyright 2015 Tautvydas Andrikys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esminis.server.mariadb.server;

import android.content.Context;

import com.esminis.server.library.service.server.ServerLauncher;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MariaDbServerLauncher extends ServerLauncher {

	private final Object lock = new Object();

	MariaDbServerLauncher(com.esminis.server.library.model.manager.Process managerProcess) {
		super(managerProcess);
	}

	Process start(
		File binary, String address, File documentRoot, boolean keepRunning, Context context
	) throws IOException {
		return start(
			binary, createCommand(context, binary, address, documentRoot), context, getEnvironment(),
			documentRoot, keepRunning
		);
	}

	void initializeDataDirectory(Context context, File binary, File root) throws IOException {
		synchronized (lock) {
			final List<String> environment = getEnvironment();
			final List<String> command = createCommandInternal(context, binary, root);
			Collections.addAll(
				command, "--bootstrap", "--log-warnings=0", "--loose-skip-ndbcluster",
				"--max_allowed_packet=8M", "--net_buffer_length=16K"
			);
			final File dataMysqlDirectory = new File(root, "mysql");
			if (!dataMysqlDirectory.isDirectory() && !dataMysqlDirectory.mkdirs()) {
				throw new IOException("Cannot create directory: " + dataMysqlDirectory.getAbsolutePath());
			}
			final Process process = Runtime.getRuntime().exec(
				command.toArray(new String [command.size()]),
				environment.toArray(new String[environment.size()]), root
			);
			final OutputStream stream = process.getOutputStream();
			writeToStream(stream, "use mysql;\n");
			writeToStream(stream, context, "sql/mysql_system_tables.sql");
			writeToStream(stream, context, "sql/mysql_performance_tables.sql");
			writeToStream(stream, context, "sql/mysql_system_tables_data.sql");
			writeToStream(stream, context, "sql/fill_help_tables.sql");
			writeToStream(stream, "exit;\n");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
	}

	private void writeToStream(
		OutputStream outputStream, Context context, String assetPath
	) throws IOException {
		writeToStream(outputStream, IOUtils.toString(context.getAssets().open(assetPath)));
	}

	private void writeToStream(OutputStream outputStream, String content) throws IOException {
		outputStream.write(content.getBytes());
	}

	private List<String> createCommandInternal(
		Context context, File binary, File root
	) throws IOException {
		final List<String> command = new ArrayList<>();
		final File directoryTemp = new File(context.getExternalFilesDir(null), "temp");
		final File fileSocket = new File(binary.getParentFile(), "mysql.sock");
		if (!directoryTemp.isDirectory() && !directoryTemp.mkdirs()) {
			throw new IOException("Cannot create directory: " + directoryTemp.getAbsolutePath());
		}
		command.add(binary.getAbsolutePath());
		command.add("--lc-messages-dir=" + new File(binary.getParentFile(), "share"));
		command.add("--tmpdir=" + directoryTemp.getAbsolutePath());
		command.add("--socket=" + fileSocket);
		command.add("--basedir=" + binary.getParentFile().getAbsolutePath());
		command.add("--datadir=" + root.getAbsolutePath());
		command.add("--lower-case-table-names=1");
		return command;
	}

	private List<String> createCommand(
		Context context, File binary, String address, File root
	) throws IOException {
		final List<String> command = createCommandInternal(context, binary, root);
		final String[] addressParts = address.split(":");
		command.add("--bind-address=" + addressParts[0]);
		command.add("--port=" + addressParts[1]);
		return command;
	}

}