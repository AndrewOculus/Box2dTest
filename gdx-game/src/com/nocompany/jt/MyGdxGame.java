package com.nocompany.jt;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.utils.*;
import java.util.*;
import com.badlogic.gdx.physics.box2d.joints.*;
import android.util.*;

public class MyGdxGame implements ApplicationListener
{
	PhysicsWorld physicsWorld;
	OrthographicCamera camera;
	ShapeRenderer sr;
	GameObject box;
	GameObject hero;

	@Override
	public void create()
	{
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.zoom = 0.02f;
		physicsWorld = new PhysicsWorld(camera);
		
		GameObject.Builder builder = new GameObject.Builder(physicsWorld);
	
		box = builder.setPos(new Vector2(-10,0))
			.setSize(new Vector2(30,0.5f))
			.setType(BodyDef.BodyType.StaticBody)
			.build();
			
		hero = builder.setPos(new Vector2(0,10))
			.setSize(new Vector2(1.1f,1.1f))
			.setType(BodyDef.BodyType.DynamicBody)
			.setMassData(1,0.1f)
			.build();
		
		hero.addAction(new Action(){
			
			private Vector2 imp = new Vector2(0,0);
			private Vector3 camPos = new Vector3();
			
			public void act(float dt, GameObject self){
				imp.set(self.body.getLinearVelocity());
				imp.x = Gdx.input.getDeltaX()*5;
				self.body.setLinearVelocity(imp);
				camPos.x = self.body.getPosition().x;
				camPos.y = self.body.getPosition().y;
				camera.position.set(camera.position.lerp(camPos,0.05f));
				camera.update();
			}
		});
		
		sr = new ShapeRenderer();
	}

	@Override
	public void render()
	{        
	    Gdx.gl.glClearColor(1, 1, 1, 1);
	    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		float dt = Gdx.graphics.getDeltaTime();
		physicsWorld.update(dt);
		physicsWorld.render(sr);
		camera.update();
	}

	@Override
	public void dispose()
	{
		physicsWorld.dispose();
		sr.dispose();
	}

	@Override
	public void resize(int width, int height)
	{
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void resume()
	{
	}
}

class PhysicsWorld implements Disposable
{

	@Override
	public void dispose(){
		world.dispose();
		box2ddr.dispose();
	}

	private World world = new World(new Vector2(0,-9.81f), false);
	private Box2DDebugRenderer box2ddr = new Box2DDebugRenderer();
	private OrthographicCamera camera;
	private Array<Body> bodies = new Array<Body>();
	
	public PhysicsWorld(OrthographicCamera camera){
		this.camera = camera;
	}
	
	public void update(float dt){
		world.getBodies(bodies);
		for(Body body : bodies){
			Object usrData=body.getUserData();
			if(usrData instanceof GameObject){
				((GameObject)usrData).updateActions(dt);
			}
		}
		
		world.step(dt,2,6);
	}
	public void render(ShapeRenderer sr){
		box2ddr.render(world,camera.combined);
	}
	public Body istantiate(Vector2 pos, Vector2 size, BodyDef.BodyType type, ShapeType shType, MassData massData){
		BodyDef bodyDef = new BodyDef();
        bodyDef.type = type;
        bodyDef.position.set(pos.x, pos.y);
		Body body = this.world.createBody(bodyDef);
		body.setMassData(massData);
		
		if(shType == ShapeType.box){
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(size.x, size.y);
			FixtureDef fixtureDef = new FixtureDef();
			fixtureDef.shape = shape;
			fixtureDef.density = 1f;
			Fixture fixture = body.createFixture(fixtureDef);
			shape.dispose();
		}
       	else{
			CircleShape shape = new CircleShape();
			shape.setRadius(size.x);
			FixtureDef fixtureDef = new FixtureDef();
			fixtureDef.shape = shape;
			fixtureDef.density = 1f;
			Fixture fixture = body.createFixture(fixtureDef);
			shape.dispose();
		}
	
		return body;
	}
	
	public World getWorld(){
		return world;
	}
}

enum ShapeType{
	box, circle
}

class Action{
	public void act(float dt, GameObject self){}
}

class GameObject{
	
	public Body body;
	public DistanceJoint joint;
	private World world;
	private List<Action> actions;
	
	public GameObject(){
		actions = new ArrayList<Action>();
	}
	
	public void addAction(Action act){
		actions.add(act);
		body.setUserData(this);
	}
	
	public List<Action> getActions(){
		return actions;
	}
	
	public void updateActions(float dt){
		for(Action act : actions){
			act.act(dt,this);
		}
	}
	
	public void setWorld(World World){
		this.world=World;
	}
	
	public void makeDistanceJoint(GameObject B){
		makeDistanceJoint(B, this.body.getPosition(),B.body.getPosition());
	}
	
	public void makeDistanceJoint(GameObject B, Vector2 p1, Vector2 p2){
		if(joint!=null)
			return;
		DistanceJointDef defJoint = new DistanceJointDef ();
		defJoint.length = 0;
		defJoint.frequencyHz=10;
		defJoint.dampingRatio=0;
		//defJoint.collideConnected=true;
		defJoint.initialize(this.body, B.body, p1, p2);
		joint = (DistanceJoint) world.createJoint(defJoint);
	}
	
	public void destroyDistanceJoint(){
		if(joint!=null){
			world.destroyJoint(joint);
			joint = null;
		}
	}
	
	public static class Builder{
		
		private GameObject gameObject;
		private PhysicsWorld physicsWorld;
		private Vector2 position = new Vector2(0,0);
		private Vector2 size = new Vector2(10,10);
		private BodyDef.BodyType type = BodyDef.BodyType.DynamicBody;
		private ShapeType shapeType = ShapeType.box;
		private MassData massData = new MassData();
		
		public Builder(PhysicsWorld pw){
			this.physicsWorld = pw;
		}
		
		public Builder setPos(Vector2 pos){
			this.position = pos;
			return this;
		}
		
		public Builder setSize(Vector2 size){
			this.size = size;
			return this;
		}
		
		public Builder setType(BodyDef.BodyType type){
			this.type = type;
			return this;
		}
		
		public Builder setShape(ShapeType shapeType){
			this.shapeType=shapeType;
			return this;
		}
		
		public Builder setMassData(float mass, float I){
			this.massData.I=I;
			this.massData.mass=mass;
			return this;
		}
		
		public Builder setMassData(float mass, float I, Vector2 center){
			this.massData.I=I;
			this.massData.mass=mass;
			this.massData.center.set(center);
			return this;
		}
		
		public GameObject build(){
			gameObject = new GameObject();
			gameObject.setWorld(physicsWorld.getWorld());
			gameObject.body = physicsWorld.istantiate(position,size,type,shapeType,massData);
			return gameObject;
		}
	}
	
}
