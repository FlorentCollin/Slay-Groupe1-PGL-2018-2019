package logic.naturalDisasters.naturalDisasterscontroller;

import java.util.ArrayList;

import logic.board.Board;
import logic.naturalDisasters.Blizzard;
import logic.naturalDisasters.Drought;
import logic.naturalDisasters.ForestFire;
import logic.naturalDisasters.LandErosion;
import logic.naturalDisasters.NaturalDisasters;
import logic.naturalDisasters.Tsunami;
import logic.naturalDisasters.VolcanicEruption;

public class NaturalDisastersController {
	private ArrayList<NaturalDisasters> enableDisasters;
	private ArrayList<NaturalDisasters> naturalDisasters;
	private Blizzard blizzard;
	private Drought drought;
	private ForestFire forestFire;
	private LandErosion landErosion;
	private Tsunami tsunami;
	private VolcanicEruption volcanicEruption;
	
	public NaturalDisastersController(Board board) {
		enableDisasters = new ArrayList<>();
		naturalDisasters = new ArrayList<>();
		blizzard = new Blizzard(board);
		naturalDisasters.add(blizzard);
		drought = new Drought(board);
		naturalDisasters.add(drought);
		forestFire = new ForestFire(board);
		naturalDisasters.add(forestFire);
		landErosion = new LandErosion(board);
		naturalDisasters.add(landErosion);
		tsunami = new Tsunami(board);
		naturalDisasters.add(tsunami);
		volcanicEruption = new VolcanicEruption(board);
		naturalDisasters.add(volcanicEruption);
	}
	
	public void isHappening() {
		for(NaturalDisasters nd : naturalDisasters) {
			nd.play();
			if(nd.getAffectedCells().size() > 0) {
				enableDisasters.add(nd);
			}
		}
	}
}