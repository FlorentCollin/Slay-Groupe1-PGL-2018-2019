package gui.graphics.screens;


import static gui.utils.Constants.N_TILES;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FillViewport;

import communication.Message;
import communication.MessageListener;
import communication.MessageSender;
import communication.PlayMessage;
import communication.ShopMessage;
import communication.TextMessage;
import gui.Hud;
import gui.app.Slay;
import gui.utils.Map;
import logic.Coords.OffsetCoords;
import logic.Coords.TransformCoords;
import logic.board.Board;
import logic.board.cell.Cell;
import logic.item.Item;
import logic.item.Soldier;
import logic.item.level.SoldierLevel;
import logic.player.Player;
import roomController.Room;

public class InGameScreen extends BasicScreen implements InputProcessor {

    private Map map;
    private Vector3 mouseLoc = new Vector3();
    private float worldWith; // ?
    private float worldHeight;
    private TiledMapTileLayer cells;
    private TiledMapTileLayer selectedLayer;
    private volatile Board board;
    private TextureAtlas itemsSkin;
    private ArrayList<Cell> selectedCells = new ArrayList<>();
    private FillViewport fillViewport;
    private Hud hud;

    private LinkedBlockingQueue<Message> messagesFrom;
    private LinkedBlockingQueue<Message> messagesToSend;
    private MessageListener messageListener;
    private MessageSender messageSender;
    private Room room;

    public InGameScreen(Slay parent, String mapName, Board board, MessageSender messageSender) {
        super(parent);
        this.messageSender = messageSender;
        this.board = board;
        //Chargement du TmxRenderer et des textures
        itemsSkin = new TextureAtlas(Gdx.files.internal("items/items.atlas"));
        map = new Map();
        map.load(mapName, false, true, true);
        cells = map.getCells();
        selectedLayer = map.getSelectedCells();
        //Calcule de la grandeur de la carte
        worldWith = (cells.getWidth()/2) * cells.getTileWidth() + (cells.getWidth() / 2) * (cells.getTileWidth() / 2) + cells.getTileWidth()/4;
        worldHeight = cells.getHeight() * cells.getTileHeight() + cells.getTileHeight() / 2;

        hud = new Hud(this, itemsSkin);
        Hud.Shop shop = hud.getShop();
        addListener(shop.soldierLvl1, new Soldier(SoldierLevel.level1));
        addListener(shop.soldierLvl2, new Soldier(SoldierLevel.level2));
        addListener(shop.soldierLvl3, new Soldier(SoldierLevel.level3));
        addListener(shop.soldierLvl4, new Soldier(SoldierLevel.level4));
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hud);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
    }

    public InGameScreen(Slay parent, String mapName, Board board, MessageSender messageSender, MessageListener messageListener) {
        this(parent, mapName, board, messageSender);
        this.messageListener = messageListener;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        changeModifiedCells();
        if(board.getSelectedCell() != null) {
            selectCells(board.possibleMove(board.getSelectedCell()));
        }
        map.getTiledMapRenderer().setView(camera);
        map.getTiledMapRenderer().render(); //Rendering des cellules
        renderItems();
        hud.getViewport().apply();
        hud.act(delta);
        hud.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        hud.getViewport().update(width, height, true);
        hud.getBatch().setProjectionMatrix(hud.getCamera().combined);

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
    }

    private void addListener(Actor actor, Item item) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                messageSender.send(new ShopMessage(item));
            }
        });
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
    public void dispose() {
        messageListener.stopRunning();
        room.stopRunning();
        super.dispose();
    }


    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.ENTER) {
            messageSender.send(new TextMessage("nextPlayer"));
        } else if(keycode == Input.Keys.ESCAPE) {
            parent.changeScreen(MainMenuScreen.class);
        }
        return true;
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
            messageSender.send(new PlayMessage(boardCoords.col, boardCoords.row));
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
                if(cell != null) {
                	// Ne s'applique que si la case appartient au joueur
                	// Ainsi il voit directement avec quelles cases il peut interagir
                    if(cell.getDistrict() != null && cell.getDistrict().getPlayer().getId() == board.getActivePlayer().getId()) {
                        cells.setOpacity(0.7f);
                        selectCells(cell.getDistrict().getCells());
                    } else {
                        cells.setOpacity(1f);
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
            // On récupère les coordonnées dans la mapTmx car celles-ci sont différentes des coordonnées dans le board
            OffsetCoords tmxCoords = boardToTmxCoords(new OffsetCoords(cell.getX(), cell.getY()));
            // Récupération de la cellule dans la
            TiledMapTileLayer.Cell tmxCell = cells.getCell(tmxCoords.col, tmxCoords.row);
            TiledMapTileLayer.Cell tmxSelectedCell = selectedLayer.getCell(tmxCoords.col, tmxCoords.row);
            // On change la tile (l'image) de la cellule à sélectionner.

            tmxSelectedCell.setTile(map.getTileSetSelected().getTile(tmxCell.getTile().getId()+N_TILES));

        }
    }

    private void unselectCells() {
    	int numberPlayer = -1;
        for (Cell selectedCell : selectedCells) {

            // On récupère les coordonnées dans la mapTmx car celles-ci sont différentes des coordonnées dans le board
            OffsetCoords tmxCoords = boardToTmxCoords(new OffsetCoords(selectedCell.getX(),selectedCell.getY()));
            // Récupération de la cellule dans la mapTmx

            TiledMapTileLayer.Cell tmxSelectedCell = selectedLayer.getCell(tmxCoords.col, tmxCoords.row);
            // On change la tile (l'image) de la cellule à sélectionner.
            tmxSelectedCell.setTile(null);
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
                            if(tmxCell.getTile().getId()-1 != playerNumber && tmxCell.getTile().getId()-1-N_TILES != playerNumber) {
                                tmxCell.setTile(map.getTileSet().getTile(playerNumber+1));
                                break;
                            }
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
