/* Copyright 2013 Ivan Iljkic
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/
package net.utp4j.examples.configtest;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.utp4j.channels.impl.alg.PacketSizeModus;
import net.utp4j.channels.impl.alg.UtpAlgConfiguration;
import net.utp4j.channels.impl.log.UtpDataLogger;

public class ConfigTestPlanReader {

	private InputStream fis;
	private Deque<String> testParameters = new LinkedList<String>();
	private int testRun = 0;
	private String lastParameters;
	private SimpleDateFormat format = new SimpleDateFormat("dd_MM_hh_mm_ss");
	
	private static final Logger log = LoggerFactory.getLogger(ConfigTestPlanReader.class);

	public ConfigTestPlanReader(String fileLocation) throws FileNotFoundException {
		this(new FileInputStream(fileLocation));
	}

	public ConfigTestPlanReader(InputStream inputStream) {
		this.fis = inputStream;
		UtpAlgConfiguration.DEBUG = false;
	}
	
	public void read() throws IOException {
		BufferedReader br;
		String line;

		br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
		boolean skipLine = true;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
			if (!skipLine) {
				String[] split = line.split(";");
				int repetitions = Integer.parseInt(split[split.length - 1].trim());
				for (int i = 0; i < repetitions; i++) {
					testParameters.add(line);				
				}				
			} else {
				skipLine = false;
			}
		}
		br.close();
		br = null;
		fis = null;
	}
	
	public String next() {
		testRun++;
		Date date = new Date();
		String dateString = format.format(date);
		String logname = "auto/auto_run_" + testRun + "_" + dateString;
		UtpDataLogger.LOG_NAME = logname + ".csv";
		String parameters = testParameters.remove();
		lastParameters = parameters;
		String[] splitParameters = parameters.split(";");
		log.debug(Arrays.toString(splitParameters));
		UtpAlgConfiguration.MINIMUM_TIMEOUT_MILLIS = Integer.parseInt(splitParameters[0]);
		UtpAlgConfiguration.PACKET_SIZE_MODE = parsePktSizeMode(splitParameters[1]);
		UtpAlgConfiguration.MAX_PACKET_SIZE = Integer.parseInt(splitParameters[2]);
		UtpAlgConfiguration.MIN_PACKET_SIZE = Integer.parseInt(splitParameters[3]);
		UtpAlgConfiguration.MINIMUM_MTU = Integer.parseInt(splitParameters[4]);
		UtpAlgConfiguration.MAX_CWND_INCREASE_PACKETS_PER_RTT = Integer.parseInt(splitParameters[5]);
		UtpAlgConfiguration.C_CONTROL_TARGET_MICROS = Integer.parseInt(splitParameters[6]);
		UtpAlgConfiguration.SEND_IN_BURST = toBool(splitParameters[7]);
		UtpAlgConfiguration.MAX_BURST_SEND = Integer.parseInt(splitParameters[8]);
		UtpAlgConfiguration.MIN_SKIP_PACKET_BEFORE_RESEND = Integer.parseInt(splitParameters[9]);
		UtpAlgConfiguration.MICROSECOND_WAIT_BETWEEN_BURSTS = Integer.parseInt(splitParameters[10]);
		UtpAlgConfiguration.TIME_WAIT_AFTER_LAST_PACKET = Integer.parseInt(splitParameters[11]);
		UtpAlgConfiguration.ONLY_POSITIVE_GAIN = toBool(splitParameters[12]);
		// new exposed UTP algorithm parameters for receiver
		UtpAlgConfiguration.SKIP_PACKETS_UNTIL_ACK = Integer.parseInt(splitParameters[13]);
		UtpAlgConfiguration.AUTO_ACK_SMALLER_THAN_ACK_NUMBER = toBool(splitParameters[14]);

		UtpAlgConfiguration.DEBUG = true;
		return parameters + " -- " + dateString;
		
	}
	
	private PacketSizeModus parsePktSizeMode(String strg) {
		if ("DYNAMIC_LINEAR".equals(strg)) return PacketSizeModus.DYNAMIC_LINEAR;
		else if ("CONSTANT_1472".equals(strg)) return PacketSizeModus.CONSTANT_1472;
		else if ("CONSANT_576".equals(strg)) return PacketSizeModus.CONSTANT_576;
		return null;
	}
	
	private boolean toBool(String strg) {
		return "1".equals(strg);
	}
	
	public boolean hasNext() {
		return !testParameters.isEmpty();
	}
	public void failed() {
		if (lastParameters != null) {
			testParameters.addFirst(lastParameters);			
			testRun--;
		}
	}
}
