/*******************************************************************************
 * Copyright (c) 2015 - 2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.mapcreator.mapvalidator;

import java.util.ArrayList;
import java.util.List;

import jsettlers.mapcreator.data.MapData;
import jsettlers.mapcreator.mapvalidator.result.ValidationList;
import jsettlers.mapcreator.mapvalidator.tasks.AbstractValidationTask;
import jsettlers.mapcreator.mapvalidator.tasks.ValidateBlockingBorderPositions;
import jsettlers.mapcreator.mapvalidator.tasks.ValidateBuildings;
import jsettlers.mapcreator.mapvalidator.tasks.ValidateDrawBuildingCircle;
import jsettlers.mapcreator.mapvalidator.tasks.ValidateLandscape;
import jsettlers.mapcreator.mapvalidator.tasks.ValidatePlayerStartPosition;
import jsettlers.mapcreator.mapvalidator.tasks.ValidateResources;
import jsettlers.mapcreator.mapvalidator.tasks.ValidateSettler;

/**
 * The validation runnable running in the thread queue
 * 
 * @author Andreas Butti
 */
public class ValidatorRunnable implements Runnable {

	/**
	 * List with the errors
	 */
	private ValidationList list = new ValidationList();

	/**
	 * Listener for validation result
	 */
	private ValidationResultListener resultListener;

	/**
	 * Validation tasks
	 */
	private List<AbstractValidationTask> tasks = new ArrayList<>();

	/**
	 * Map to check
	 */
	private final MapData data;

	/**
	 * Player data array
	 */
	protected byte[][] players;

	/**
	 * Border array
	 */
	protected boolean[][] borders;

	/**
	 * Constructor
	 * 
	 * @param resultListener
	 *            Listener for validation result
	 * @param data
	 *            Map to check
	 */
	public ValidatorRunnable(ValidationResultListener resultListener, MapData data) {
		this.resultListener = resultListener;
		this.data = data;

		// keep order, will be executed in this order
		registerTask(new ValidateBlockingBorderPositions());
		registerTask(new ValidateDrawBuildingCircle());
		registerTask(new ValidateBuildings());
		registerTask(new ValidateSettler());
		registerTask(new ValidateLandscape());
		registerTask(new ValidateResources());
		registerTask(new ValidatePlayerStartPosition());
	}

	/**
	 * Constructor for UnitTesting, execute only a single task
	 * 
	 * @param resultListener
	 *            Listener for validation result
	 * @param data
	 *            Map to check
	 * @param tasks
	 *            Tasks to execute
	 */
	public ValidatorRunnable(ValidationResultListener resultListener, MapData data, List<AbstractValidationTask> tasks) {
		this.resultListener = resultListener;
		this.data = data;

		for (AbstractValidationTask t : tasks) {
			registerTask(t);
		}
	}

	/**
	 * Initialize the player array
	 */
	private void initPlayerData() {
		players = new byte[data.getWidth()][data.getHeight()];
		for (int x = 0; x < data.getWidth(); x++) {
			for (int y = 0; y < data.getHeight(); y++) {
				players[x][y] = (byte) -1;
			}
		}

		borders = new boolean[data.getWidth()][data.getHeight()];
	}

	/**
	 * Register a task to be executed
	 * 
	 * @param task
	 *            Task
	 */
	private void registerTask(AbstractValidationTask task) {
		tasks.add(task);
		task.setData(data);
		task.setList(list);
	}

	@Override
	public void run() {
		initPlayerData();

		for (AbstractValidationTask task : tasks) {
			task.setPlayers(players);
			task.setBorders(borders);
			task.doTest();
		}

		data.setPlayers(players);
		data.setBorders(borders);
		data.setFailpoints(new boolean[data.getWidth()][data.getHeight()]);

		// fire result to UI
		resultListener.validationFinished(list.toListModel());
	}
}