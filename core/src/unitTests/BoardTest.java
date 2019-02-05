package unitTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import logic.board.Board;
import logic.board.District;
import logic.item.Soldier;
import logic.item.level.SoldierLevel;
import logic.naturalDisasters.NaturalDisastersController;
import logic.player.Player;
import logic.shop.Shop;

public class BoardTest {
	Board board;
	Player p1, p2;
	Shop shop;
	District district, district2;
	
	@Before
	public void init() {
		int rows = 5;
		int columns = 5;
		p1 = new Player();
		p2 = new Player();
		Player[] players = new Player[2];
		players[0] = p1;
		players[1] = p2;
		NaturalDisastersController naturalDisastersController = new NaturalDisastersController();
		shop = new Shop();
		board = new Board(rows,columns, players, naturalDisastersController, shop);
		district = new District(p1);
		district2 = new District(p2);
		for(int i=0; i<2; i++) {
			for(int j=0; j<2; j++) {
				district.addCell(board.getCell(i, j));
				board.getCell(i, j).setDistrict(district);
			}
		}
		for(int i = 2; i<4; i++) {
			for(int j = 2; j<4; j++) {
				district2.addCell(board.getCell(i, j));
				board.getCell(i, j).setDistrict(district2);
			}
		}
		board.setActiveDistrict(district);
	}
	
	@Test
	public void testPlaceNewItemOnFreeTerritory() {
		shop.setSelectedItem(new Soldier(p1));
		board.placeNewItem(2, 0);
		assertTrue(board.getCell(2, 0).getDistrict() == district);
	}
	
	@Test 
	public void testPlaceNewItemOnOwnTerritory() {
		Soldier soldier = new Soldier(p1);
		shop.setSelectedItem(soldier);
		board.placeNewItem(0, 0);
		assertTrue(board.getCell(0, 0).getItem() == soldier);
	}
	
	@Test
	public void testFusion() {
		Soldier soldier = new Soldier(p1);
		shop.setSelectedItem(soldier);
		board.placeNewItem(0, 0);
		Soldier soldier2 = new Soldier(p1);
		shop.setSelectedItem(soldier2);
		board.placeNewItem(0, 0);
		assertTrue(board.getCell(0, 0).getItem().getLevel() == SoldierLevel.level2);
	}
	
	@Test
	public void testNotFusion() {
		Soldier soldier = new Soldier(p1, SoldierLevel.level2);
		shop.setSelectedItem(soldier);
		board.placeNewItem(0, 0);
		Soldier soldier2 = new Soldier(p1);
		shop.setSelectedItem(soldier2);
		board.placeNewItem(0, 0);
		assertTrue(board.getCell(0, 0).getItem() == soldier);
	}
	
	@Test
	public void testPlaceOnEmptyEnemeyTerritory() {
		Soldier soldier = new Soldier(p1);
		shop.setSelectedItem(soldier);
		board.placeNewItem(2, 2);
		assertTrue(board.getCell(2, 2).getDistrict() == board.getActiveDistrict());
	}
	
	@Test
	public void testPlaceOnEnemySoldierOfSameLevel() {
		Soldier soldier = new Soldier(p1);
		shop.setSelectedItem(soldier);
		board.placeNewItem(1, 1);
		Soldier soldier2 = new Soldier(p2);
		board.setActiveDistrict(district2);
		shop.setSelectedItem(soldier2);
		board.placeNewItem(1, 1);
		assertTrue(board.getCell(1, 1).getDistrict() == board.getActiveDistrict() && board.getCell(1, 1).getItem() == soldier2);
	}
	
	@Test
	public void testPlaceOnEnemySoldierOfUpperLevel() {
		Soldier soldier = new Soldier(p1, SoldierLevel.level3);
		shop.setSelectedItem(soldier);
		board.placeNewItem(1, 1);
		Soldier soldier2 = new Soldier(p2);
		board.setActiveDistrict(district2);
		shop.setSelectedItem(soldier2);
		board.placeNewItem(1, 1);
		assertTrue(board.getCell(1, 1).getDistrict() == district && board.getCell(1, 1).getItem() == soldier);
	}
	
	@Test
	public void testPossibleMove() {
		
	}

}
