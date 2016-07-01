import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.junit.Test;


public class SpaceAssignerTest {
	
	
	private static final SpaceAssigner createTestEnvironment(List<Space> spaces) {
		ParkingGarage garage = new ParkingGarageImpl(spaces);
		SpaceAssigner assigner = new SpaceAssigner(spaces.size());
		assigner.initialize(garage);
		return assigner;
	}
	

	@Test
	public void testAssignSpaceReturnsSpaceWithHighestDesirability() {
		// given an empty garage with 3 spaces
		SpaceAssigner assigner = createTestEnvironment(Arrays.asList(
			SpaceImpl.create(1, 1),
			SpaceImpl.create(2, 2),
			SpaceImpl.create(3, 3)
		));
		// when a car enters the garage
		Space reservation = assigner.assignSpace(new CarImpl("WA", "1234"));
		// then the reserved space should be the one with the highest desirability
		assertTrue(reservation.getDesirability() == 3);
	}
	
	
	@Test
	public void testTwoCarsDontGetTheSameSpace() throws Exception {
		// given an empty garage with 1000 spaces
		List<Space> spaces = new ArrayList<Space>(); 
		
		for (int i = 0 ; i < 1000 ; i++) {
			spaces.add(SpaceImpl.create(i, 4));
		}
		
		final SpaceAssigner assigner = createTestEnvironment(spaces);
		// when the same amount of cars as spaces enter the garage in parallel
		Executor executor = Executors.newFixedThreadPool(12);
		CompletionService<Space> completionService = 
			       new ExecutorCompletionService<Space>(executor);
		for (Space s: spaces) {
			completionService.submit(new Callable<Space>() {
				@Override
				public Space call() throws Exception {
					Car car = new CarImpl(UUID.randomUUID().toString(), UUID.randomUUID().toString());
					return assigner.assignSpace(car);
				}
			});
		}
		int results = 0;
		Set<Space> reservations = new HashSet<Space>();
		while (results < spaces.size()) {
			reservations.add(completionService.take().get());
			results += 1;
		}
		// Then a space should not be reserved by more than one car
		assertTrue(reservations.size() == spaces.size());
	}
	
	@Test
	public void testCarDoesNotGetSpaceWhenAllSpacesAreOccupied() {
		// Given a garage with 3 occupied spaces
		SpaceAssigner assigner = createTestEnvironment(Arrays.asList(
			SpaceImpl.create(1, 1, new CarImpl("WA", "1234")),
			SpaceImpl.create(2, 2, new CarImpl("WA", "1236")),
			SpaceImpl.create(3, 3, new CarImpl("WA", "1235"))
		));
		// When a forth car tries to enter the garage
		Car car = new CarImpl("WA", "1237");
		Space reservation = assigner.assignSpace(car);
		// Then it should not get a reservation
		assertTrue(reservation == null);
	}
	
	@Test
	public void testReserveSpaceAsSoonAsOneIsFreed() {
		// Given a garage with 3 occupied spaces
		Car leaving = new CarImpl("WA", "1234");
		Space space = SpaceImpl.create(1, 1, leaving); 
		SpaceAssigner assigner = createTestEnvironment(Arrays.asList(
			space,
			SpaceImpl.create(2, 2, new CarImpl("WA", "1236")),
			SpaceImpl.create(3, 3, new CarImpl("WA", "1235"))
		));
		// When a car leaves its space and a forth car enters the garage
		assigner.onSpaceFreed(leaving, space);
		Car entering = new CarImpl("CA", "43321");
		Space reservation = assigner.assignSpace(entering);
		// Then the space freed by the car leaving should be reserved by the car entering
		assertEquals(space, reservation);
	}
	
	@Test
	public void testCarParksAtAntoherSpace() {
		// Given an empty garage with 3 spaces
		Car first = new CarImpl("WA", "1234");
		Space other = SpaceImpl.create(1, 1); 
		SpaceAssigner assigner = createTestEnvironment(Arrays.asList(
			other,
			SpaceImpl.create(2, 2),
			SpaceImpl.create(3, 3)
		));
		// When a car enters the garage and parks in a space other than the one reserved
		Space reserved = assigner.assignSpace(first);
		assigner.onSpaceTaken(first, other);
		// Then the space reserved for the first car should be available for a second car
		Car second = new CarImpl("CA", "43321");
		assertEquals(assigner.assignSpace(second), reserved);
	}
	
	
	
	
	
	
	
	
	
	static class SpaceImpl implements Space {
		
		static Space create(int id, int desirability) {
			return new SpaceImpl(id, desirability, null);
		}
		
		static Space create(int id, int desirability, Car occupant) {
			return new SpaceImpl(id, desirability, occupant);
		}
		
		private final int id;
		private final int desirability;
		
		private Car occupant;
		
		private SpaceImpl(int id, int desirability, Car occupant) {
			this.id = id;
			this.desirability = desirability;
			this.occupant = occupant;
		}

		@Override
		public int getID() {
			return id;
		}

		@Override
		public int getDesirability() {
			return desirability;
		}

		@Override
		public boolean isOccupied() {
			return occupant != null;
		}

		@Override
		public Car getOccupyingCar() {
			return occupant;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SpaceImpl other = (SpaceImpl) obj;
			if (id != other.id)
				return false;
			return true;
		}
		
		
	}
	
	static class CarImpl implements Car {

		private final String plateState;
		private final String plateNumber;
		
		public CarImpl(String plateState, String plateNumber) {
			this.plateState = plateState;
			this.plateNumber = plateNumber;
		}
		
		@Override
		public String getLicensePlateState() {
			return plateState;
		}

		@Override
		public String getLicensePlateNumber() {
			return plateNumber;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((plateNumber == null) ? 0 : plateNumber.hashCode());
			result = prime * result
					+ ((plateState == null) ? 0 : plateState.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CarImpl other = (CarImpl) obj;
			if (plateNumber == null) {
				if (other.plateNumber != null)
					return false;
			} else if (!plateNumber.equals(other.plateNumber))
				return false;
			if (plateState == null) {
				if (other.plateState != null)
					return false;
			} else if (!plateState.equals(other.plateState))
				return false;
			return true;
		}
	}
	
	static class ParkingGarageImpl implements ParkingGarage {
		
		private final List<Space> spaces;
		
		public ParkingGarageImpl(List<Space> spaces) {
			this.spaces = spaces;
		}

		@Override
		public void register(GarageStatusListener assigner) { }

		@Override
		public Iterator<Space> getSpaces() {
			return spaces.iterator();
		}
		
	}
	
	


}
