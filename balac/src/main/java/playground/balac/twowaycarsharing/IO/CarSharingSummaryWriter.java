package playground.balac.twowaycarsharing.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import playground.balac.twowaycarsharing.router.CarSharingStation;



public class CarSharingSummaryWriter
{
  private FileWriter fw = null;
  private BufferedWriter out = null;

  public CarSharingSummaryWriter(String outfile)
  {
    try
    {
      this.fw = new FileWriter(outfile);
      System.out.println(outfile);
      this.out = new BufferedWriter(this.fw);
      this.out.write("Pers_Id\tLicense\tCar_availability\tStart_x\tstart_y\tfromStation_x\tfromStation_y\ttoStation_x\ttoStation_y\tEnd_x\tEnd_y\tDep_Time\tArr_Time\tAct_Type_B\tAct_Type_A\n");
      this.out.flush();
    } catch (IOException e) {
      e.printStackTrace();

    }
    System.out.println("    done.");
  }

  public final void close()
  {
    try
    {
      this.out.flush();
      this.out.close();
      this.fw.close();
    } catch (IOException e) {
      e.printStackTrace();

    }
  }

  public void write(PersonImpl person, LinkImpl startLink, CarSharingStation fromStation, CarSharingStation toStation, LinkImpl endLink, double departureTime, double arrivalTime, ActivityImpl actBefore, ActivityImpl actAfter)
  {
    try
    {
      this.out.write(person.getId() + "\t");
      this.out.write(person.getLicense() + "\t");
      this.out.write(person.getCarAvail() + "\t");
      this.out.write(startLink.getCoord().getX() + "\t");
      this.out.write(startLink.getCoord().getY() + "\t");
      this.out.write(fromStation.getCoord().getX() + "\t");
      this.out.write(fromStation.getCoord().getY() + "\t");
      this.out.write(toStation.getCoord().getX() + "\t");
      this.out.write(toStation.getCoord().getY() + "\t");
      this.out.write(endLink.getCoord().getX() + "\t");
      this.out.write(endLink.getCoord().getY() + "\t");
      this.out.write(departureTime + "\t");
      this.out.write(arrivalTime + "\t");
      this.out.write(actBefore.getType() + "\t");
      this.out.write(actAfter.getType() + "\n");
      this.out.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
