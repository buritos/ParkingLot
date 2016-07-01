import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The SpaceAssigner is responsible for assigning a space for an incoming
 * car to park in. This is done by calling the assignSpace() API.
 *
 * The SpaceAssigner responds to changes in space availability by 
 * implementing the GarageStatusListener interface.
 */
public class SpaceAssigner implements GarageStatusListener
{
	
	private static class SpaceDesirabilityComparator implements Comparator<Space> 
	{
		@Override
		public int compare(Space o1, Space o2) 
		{
			if (o1.getDesirability() < o2.getDesirability()) 
			{
				return 1;
			}
			else if (o1.getDesirability() > o2.getDesirability())
			{
				return -1;
			}
			return 0;
		}
	}
	
  private final Queue<Space> free;
  private final Map<Car, Space> reserved;
  
  /**
   * This implementation assumes:
   * 1. that Space implements equals and hashcode in terms of ID.
   * 2. that car implements equals and hashcode in terms of LicensePlate.
   * 3. that cars don't change LicensePlates while in the parking. 
   * 4. that cars don't switch spaces or leave their space and return to the same space while in the parking. 
   * 
   * @param totalSpacesCount the total number of spaces available in the garage
   */
  public SpaceAssigner(int totalSpacesCount) {
	  this.free = new PriorityBlockingQueue<Space>(totalSpacesCount, new SpaceDesirabilityComparator());
	  this.reserved = new ConcurrentHashMap<Car, Space>(totalSpacesCount);
  }
  
	
  /**
   * Initiates the SpaceAssigner. This method is called only once per
   * app start-up.
   * 
   * Assumes that memory is enough to hold references to all spaces 
   * 
   * @param garage The parking garage for which you are vending spaces.
   */
  public void initialize(ParkingGarage garage)
  {
	Iterator<Space> spaces = garage.getSpaces();
	while (spaces.hasNext())
	{
		Space space = spaces.next();
		if (!space.isOccupied())
		{
			free.offer(space);
		}
		else
		{
			reserved.put(space.getOccupyingCar(), space);
		}
	}
  }

  /**
   * Assigns a space to an incoming car and returns that space.
   * 
   * @param car The incoming car that needs a space.
   * @returns The space reserved for the incoming car.
   */
  public Space assignSpace(Car car)
  {
	Space reservation = free.poll();
	if (reservation != null)
	{
		reserved.put(car, reservation);
	}
    return reservation;
  }

  /**
   * {@inheritDoc}
   */
  public void onSpaceTaken(Car car, Space space)
  {
	Space reservation = reserved.get(car);
	if (!reservation.equals(space))
	{
		free.remove(space);
		free.offer(reservation);
		reserved.put(car, space);
	}
  }

  /**
   * {@inheritDoc}
   */
  public void onSpaceFreed(Car car, Space space)
  {
	free.offer(reserved.remove(car));
  }
  
  /**
   * {@inheritDoc}
   */
  public void onGarageExit(Car car) {}
}

