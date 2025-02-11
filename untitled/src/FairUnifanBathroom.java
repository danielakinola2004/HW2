// SAO993
// DAA3652

public class FairUnifanBathroom {
	int max;
	int curr;
	int OU_UT;
	int ticketnumber;
	int nextticket;
	public FairUnifanBathroom() {
		/* Initialization:
		MAX = 7
		CURR = 0
		OU_UT = -1 for OU, 1 for UT and 0 for none */
		max = 7;
		curr = 0;
		OU_UT = 0;
		ticketnumber = 0;
		nextticket = 0;
	}

  public synchronized void enterBathroomUT() {
    // Called when a UT fan wants to enter bathroom

	  int curr_ticket = get_t();
	  while (OU_UT == -1 && curr > 0 || OU_UT == 1 && curr >= max || curr_ticket != nextticket) {
		  try {wait();}
		  catch (Exception e) {e.printStackTrace();}
	  }
	  curr++;
	  OU_UT = 1;
	  nextticket++;
  }
	
	public synchronized void enterBathroomOU() {
    // Called when a OU fan wants to enter bathroom
		int curr_ticket = get_t();

		while (OU_UT == 1 && curr > 0 || OU_UT == -1 && curr >= max || curr_ticket != nextticket) {
			try {wait();}
			catch (Exception e) {e.printStackTrace();}
		}
		curr++;
		OU_UT = -1;
		nextticket++;
	}
	
	public synchronized void leaveBathroomUT() {
    // Called when a UT fan wants to leave bathroom
		if (OU_UT == 1) {
			curr--;
			if (curr == 0) {
				OU_UT = 0;
			}
			notifyAll();
		}
	}

	public synchronized void leaveBathroomOU() {
    // Called when a OU fan wants to leave bathroom
		if (OU_UT == -1) {
			curr--;
			if (curr == 0) {
				OU_UT = 0;
			}
			notifyAll();
		}
	}

	public synchronized int get_t() {
		int val = this.ticketnumber;
		this.ticketnumber++;
		return val;
	}
}
	
