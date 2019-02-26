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

public class MyGdxGame implements ApplicationListener
{
	PhysicsWorld physicsWorld;
	OrthographicCamera camera;
	ShapeRenderer sr;
	GameObject box;
	GameObject hook;
	float ang=0;

	@Override
	public void create()
	{
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.zoom = 0.02f;
		physicsWorld = new PhysicsWorld(camera);
		
		GameObject.Builder builder = new GameObject.Builder(physicsWorld);
		
		builder.setSize(new Vector2(3f,0.5f))
			.setType(BodyDef.BodyType.StaticBody)
			.build();
		
		box = builder.setPos(new Vector2(0,3.01f))
			.setSize(new Vector2(3,0.5f))
			.setType(BodyDef.BodyType.StaticBody)
			.build();
			
		box = builder.setPos(new Vector2(-3.5f,1.5f))
			.setSize(new Vector2(0.5f,2f))
			.setType(BodyDef.BodyType.StaticBody)
			.build();
			
		box = builder.setPos(new Vector2(0,1))
			.setSize(new Vector2(0.5f,1))
			.setType(BodyDef.BodyType.DynamicBody)
			.build();
				
		hook = builder.setPos(new Vector2(8,0))
			.setSize(new Vector2(2f,0.1f))
			.setType(BodyDef.BodyType.StaticBody)
			.setShape(ShapeType.circle)
			.build();
			
		box.makeDistanceJoint(hook,box.body.getPosition(),hook.body.getPosition().add(0,2));
		
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
		if((!Gdx.input.isTouched()))
		ang+=dt*4;
		//if(Gdx.input.isTouched()){
			//box.destroyDistanceJoint();
			//box.body.setLinearVelocity(100,0);
			if(Gdx.input.isTouched())
			hook.body.setTransform(Gdx.input.getX()*camera.zoom-camera.viewportWidth*camera.zoom/2,(Gdx.graphics.getHeight()- Gdx.input.getY())*camera.zoom-camera.viewportHeight*camera.zoom/2,0);
			else
			hook.body.setTransform(hook.body.getPosition().x, hook.body.getPosition().y,ang);
		//}
		//else
			//box.makeDistanceJoint(hook);
		
	
			
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
	
	public PhysicsWorld(OrthographicCamera camera){
		this.camera = camera;
	}
	
	public void update(float dt){
		world.step(dt,2,6);
	}
	public void render(ShapeRenderer sr){
		box2ddr.render(world,camera.projection);
	}
	public Body istantiate(Vector2 pos, Vector2 size, BodyDef.BodyType type, ShapeType shType){
		BodyDef bodyDef = new BodyDef();
        bodyDef.type = type;
        bodyDef.position.set(pos.x, pos.y);
		Body body = this.world.createBody(bodyDef);
		
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
		
		public GameObject build(){
			gameObject = new GameObject();
			gameObject.setWorld(physicsWorld.getWorld());
			gameObject.body = physicsWorld.istantiate(position,size,type,shapeType);
			return gameObject;
		}
	}
	
}
