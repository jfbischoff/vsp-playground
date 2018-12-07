/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package gunnar.ihop2.utils;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class MovePics {

	public MovePics() {
	}

	public static void main(String[] args) throws IOException {

		for (int i = 0; i <= 200; i++) {
			System.out.println(i);
			final File from = new File(
					"./test/regentmatsim/matsim-output/ITERS/it." + i + "/" + i
							+ ".legHistogram_car.png");
			final File to = new File("./test/regentmatsim/hist" + i + ".png");

			FileUtils.copyFile(from, to);
			
		}
	}

}
