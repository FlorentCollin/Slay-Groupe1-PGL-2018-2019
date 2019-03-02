package gui.graphics.screens;


import static gui.utils.Constants.N_TILES;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FillViewport;

import gui.app.Slay;
import gui.utils.Map;
import gui.utils.MyMap;
import logic.Coords.OffsetCoords;
import logic.Coords.TransformCoords;
import logic.board.Board;
import logic.board.cell.Cell;
import logic.item.Item;
import logic.item.Soldier;
import logic.item.level.SoldierLevel;
import logic.player.Player;

public class InGameScreen extends BasicScreen implements InputProcessor {

    private Map map;
    private Vector3 mouseLoc = new Vector3();
    private float worldWith; // ?
    private float worldHeight;
    private TiledMapTileLayer cells;
    private Board board;
    private TextureAtlas itemsSkin;
    private ArrayList<Cell> selectedCells = new ArrayList<>();
    private FillViewport fillViewport;

    public InGameScreen(Slay parent, String mapName) {
        super(parent);
        itemsSkin = new TextureAtlas(Gdx.files.internal("items/items.atlas"));
        map = new Map();
        board = map.load(mapName);
        cells = map.getCells();
        //Calcule de la grandeur de la carte
        worldWith = (cells.getWidth()/2) * cells.getTileWidth() + (cells.getWidth() / 2) * (cells.getTileWidth() / 2) + cells.getTileWidth()/4;
        worldHeight = cells.getHeight() * cells.getTileHeight() + cells.getTileHeight() / 2;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        camera.update();
        changeModifiedCells();
        map.getTiledMapRenderer().setView(camera);
        map.getTiledMapRenderer().render(); //Rendering des cellules
        renderItems();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    private void renderItems() {
        //TODO ne pas recréer le sprite à chaque render car cela est lourd il  vaut mieux le stocker en mémoire
        Cell[][] tab = board.getBoard();
        Sprite sprite = null;
        for (int i = 0; i < board.getColumns(); i++) {
            for (int j = 0; j < board.getRows(); j++) {
                if (tab[i][j].getItem() != null) {
                    Item item = tab[i][j].getItem();
                    if(item instanceof Soldier) {
                        switch(((Soldier) item).getLevel()) {
                            case level1:
                                sprite = itemsSkin.createSprite(item.getClass().getSimpleName() + "_lvl1");
                                break;
                            case level2:
                                sprite = itemsSkin.createSprite(item.getClass().getSimpleName() + "_lvl2");
                                break;
                            case level3:
                                sprite = itemsSkin.createSprite(item.getClass().getSimpleName() + "_lvl3");
                                break;
                            case level4:
                                sprite = itemsSkin.createSprite(item.getClass().getSimpleName() + "_lvl4");
                                break;
                        }
                    } else {
                        sprite = itemsSkin.createSprite(item.getClass().getSimpleName());
                    }
                        if(sprite != null) {
                            stage.getBatch().begin();
                            Vector2 pos = TransformCoords.hexToPixel(i, j+1, (int) cells.getTileWidth() / 2);
                            pos.y = Math.abs(worldHeight - pos.y); // inversion de l'axe y
                            stage.getBatch().draw(sprite, pos.x, pos.y);
                            stage.getBatch().end();
                        }
                }

            }

        }
    }

    protected void generateStage() {
        camera = new OrthographicCamera();
        fillViewport = new FillViewport(1280, 720, camera);
        fillViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        stage = new Stage(fillViewport);
        Gdx.input.setInputProcessor(this);
    }

    private Vector3 getMouseLoc() {
        mouseLoc.x = Gdx.input.getX();
        mouseLoc.y = Gdx.input.getY();
        fillViewport.unproject(mouseLoc); //Récupération des coordonnées de la souris sur la map
        mouseLoc.y = Math.abs(worldHeight -mouseLoc.y); //On inverse l'axe des ordonnées
        //Cela permet d'avoir le repère placé en haut à gauche avec les y allant vers le bas et les x vers la droite
        return mouseLoc;
    }
    /**
     * Retourne les coordonnées de la cellule qui se trouve à la position de la souris
     * @param mouseLoc position de la souris
     * @return les coordonnées de la cellule qui est à la position de la souris
     */
    private OffsetCoords getCoordsFromMousePosition(Vector3 mouseLoc) {
        //- cells.getTileWidth() / 2 et - cells.getTileHeight() / 2 sont là pour créer le décalage de l'origine.
        // Ce qui permet de retrouver les bonnes coordonnés
        // le (int)cells.getTileWidth() /2 correspond à la taille de l'hexagone (ie la longueur de la droite qui va du
        // centre vers une des pointes de l'hexagon
        return TransformCoords.pixelToOffset((int)(mouseLoc.x - cells.getTileWidth() / 2),
                (int)(mouseLoc.y - cells.getTileHeight() / 2), (int)cells.getTileWidth() /2);
    }


    @Override
    public void show() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }


    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.ENTER) {
            board.nextPlayer();
        }
        else if(keycode == Input.Keys.Z) {
        	board.undo();
        }
        else if(keycode == Input.Keys.ESCAPE) {
        	Gdx.app.exit();
        }
        else if(keycode == Input.Keys.P) {
        	System.out.println("size : "+board.getPlayers().size());
        	for(Player player : board.getPlayers()) {
        		System.out.println(player);
        	}
        }
        if(board.getSelectedCell() != null) {
	        if(keycode == Input.Keys.NUMPAD_1) {
	        	board.getShop().setSelectedItem(new Soldier(SoldierLevel.level1), board.getSelectedCell().getDistrict());
	        }
	        else if(keycode == Input.Keys.NUMPAD_2) {
	        	board.getShop().setSelectedItem(new Soldier(SoldierLevel.level2), board.getSelectedCell().getDistrict());
	        }
	        else if(keycode == Input.Keys.NUMPAD_3) {
	        	board.getShop().setSelectedItem(new Soldier(SoldierLevel.level3), board.getSelectedCell().getDistrict());
	        }
	        else if(keycode == Input.Keys.NUMPAD_4) {
	        	board.getShop().setSelectedItem(new Soldier(SoldierLevel.level4), board.getSelectedCell().getDistrict());
	        }
	        if(board.getShop().getSelectedItem() != null) {
	        	selectCells(board.possibleMove(board.getSelectedCell().getDistrict()));
	        }
        }
        return false; // pq un boolean?
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        OffsetCoords boardCoords = getCoordsFromMousePosition(getMouseLoc());
        if(boardCoords.col >= 0 && boardCoords.col < board.getColumns()
                && boardCoords.row >= 0 && boardCoords.row < board.getRows()) {
            Cell selectedCell = board.getCell(boardCoords.col, boardCoords.row);
            board.play(selectedCell);
            if(board.getSelectedCell() != null && board.getSelectedCell().getItem() != null && board.getSelectedCell().getItem().isMovable() && board.getSelectedCell().getItem().canMove()) {
            	selectCells(board.possibleMove(selectedCell));
            }
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.translate(-Gdx.input.getDeltaX()*camera.zoom, Gdx.input.getDeltaY()*camera.zoom);
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        //Cette méthode va lorsqu'on passe la souris sur un district montrer ce district
        if(board.getSelectedCell() == null) { // On vérifie qu'aucune cellule n'a été sélectionnée pour une action dans le board
            // car sinon on ne doit pas montrer le district
            OffsetCoords boardCoords = getCoordsFromMousePosition(getMouseLoc());
            //On vérifie que les coordonnées sont bien dans les limites de la cartes
            if(boardCoords.col >= 0 && boardCoords.col < board.getColumns()
                    && boardCoords.row >= 0 && boardCoords.row < board.getRows()) {
                Cell cell = board.getCell(boardCoords.col, boardCoords.row);
                if(board.getCell(boardCoords.col, boardCoords.row) != null) {
                	// Ne s'applique que si la case appartient au joueur
                	// Ainsi il voit directement avec quelles cases il peut interagir
                    if(cell.getDistrict() != null && cell.getDistrict().getPlayer() == board.getActivePlayer()) {
                        selectCells(cell.getDistrict().getCells());
                    } else {
                        unselectCells();
                    }
                }
            }

        }
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        if(amount == -1) {
        	if(camera.zoom - 0.2 >= 0) {
        		camera.zoom -= 0.2;        		
        	}
        } else {
            camera.zoom += 0.2;
        }
        return true;
    }
    
    public Board getBoard() {
        return board;
    }

    //TODO Reformater le code pour ne faire plus qu'une seule méthode
    private void selectCells(ArrayList<Cell> cellsArray) {
        unselectCells();
        selectedCells = cellsArray;
        int numberPlayer;
        for(Cell cell : selectedCells) {
        	numberPlayer = -1;
        	if(cell.getDistrict() != null) {        		
        		numberPlayer = board.getPlayers().indexOf(cell.getDistrict().getPlayer());
        	}
            int[] pos = board.getPosition(cell);
            // On récupère les coordonnées dans la mapTmx car celles-ci sont différentes des coordonnées dans le board
            OffsetCoords tmxCoords = boardToTmxCoords(new OffsetCoords(pos[0], pos[1]));
            // Récupération de la cellule dans la mapTmx
            TiledMapTileLayer.Cell tmxCell = cells.getCell(tmxCoords.col, tmxCoords.row);
            // On change la tile (l'image) de la cellule à sélectionner.
            tmxCell.setTile(getSelectTile(map.getTileSet(), numberPlayer));
        }
    }

    private void unselectCells() {
    	int numberPlayer = -1;
        for (Cell selectedCell : selectedCells) {
        	if(selectedCell.getDistrict() != null) {
        		numberPlayer = board.getPlayers().indexOf(selectedCell.getDistrict().getPlayer());
        	}
            int[] pos = board.getPosition(selectedCell);
            // On récupère les coordonnées dans la mapTmx car celles-ci sont différentes des coordonnées dans le board
            OffsetCoords tmxCoords = boardToTmxCoords(new OffsetCoords(pos[0], pos[1]));
            // Récupération de la cellule dans la mapTmx
            TiledMapTileLayer.Cell tmxCell = cells.getCell(tmxCoords.col, tmxCoords.row);
            // On change la tile (l'image) de la cellule à désélectionner.
            tmxCell.setTile(getTile(map.getTileSet(), numberPlayer));
            numberPlayer = -1;
        }
        selectedCells = new ArrayList<>();
    }
    //Mdr c'est le code le plus dégeux que j'ai jamais fais de toute ma vie, mais c'est juste pour tester le jeu
    private void changeModifiedCells() {
        for (int i = 0; i < board.getColumns(); i++) {
            for (int j = 0; j < board.getRows(); j++) {
                Cell cell = board.getCell(i,j);
                if(cell.getDistrict() != null) {
                    int playerNumber;
                    Player player = cell.getDistrict().getPlayer();
                    for (int k = 0; k < board.getPlayers().size(); k++) {
                        if(player == board.getPlayers().get(k)) {
                            playerNumber = k;
                            OffsetCoords tmxCoords = boardToTmxCoords(new OffsetCoords(i,j));
                            TiledMapTileLayer.Cell tmxCell = cells.getCell(tmxCoords.col, tmxCoords.row);
                            tmxCell.setTile(getTile(map.getTileSet(), playerNumber));
//                            if(tmxCell.getTile().getId()-1 != playerNumber && tmxCell.getTile().getId()-1-N_TILES != playerNumber) {
//                                unselectCells();
//                                tmxCell.setTile(map.getTileSet().getTile(playerNumber+1));
//                                
//                                break;
//                            }
                        }
                    }
                }
            }

        }
    }
    
    private TiledMapTile getSelectTile(TiledMapTileSet tileSet, int playerNumber) {
    	TiledMapTile tile;
    	MapProperties properties;
    	for(int i=0; i<tileSet.size(); i++) {
    		tile = tileSet.getTile(i);
    		if(tile != null) {
    			properties = tile.getProperties();
    			if((int)properties.get("player") == playerNumber+1 && (boolean)properties.get("isSelection")) {
    				return tile;
    			}
    		}
    	}
    	return null;
    }
    
    private TiledMapTile getTile(TiledMapTileSet tileSet, int playerNumber) {
    	TiledMapTile tile;
    	MapProperties properties;
    	for(int i=0; i<tileSet.size(); i++) {
    		tile = tileSet.getTile(i);
    		if(tile != null) {
    			properties = tile.getProperties();
    			if((int)properties.get("player") == playerNumber+1 && !(boolean)properties.get("isSelection")) {
    				return tile;
    			}
    		}
    	}
    	return null;
    }

    public OffsetCoords tmxToBoardCoords(OffsetCoords tmxCoords) {
        return new OffsetCoords(tmxCoords.col, Math.abs(cells.getHeight()-1 - tmxCoords.row));
    }

    public OffsetCoords boardToTmxCoords(OffsetCoords boardCoords) {
        return new OffsetCoords(boardCoords.col, Math.abs(cells.getHeight()-1 - boardCoords.row));
    }
}
