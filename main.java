import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.Duration;
class CoordinatesXY {
    public double x;
    public double y;

    public CoordinatesXY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public CoordinatesXY() {
        this.x = 0;
        this.y = 0;
    }

    public boolean equals(CoordinatesXY other) {
        return this.x == other.x && this.y == other.y;
    }
}

class utils{
    public static double calculateDistance(CoordinatesXY xy1, CoordinatesXY xy2) {
        double dx = xy2.x - xy1.x;
        double dy = xy2.y - xy1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    public static boolean nodeContainsPlanet(Planet planet, Square root) {
        // Could be a better way to do this with parents
        for (long index : root.planetIndexes) {
            if (planet.equals(root.planets.get((int) index))) {
                return true;
            }
        }
        return false;
    }
    public static double calculateGravity(double m1, double m2, double distance) {
        final double G = 6.673e-11;
        return (G * m1 * m2) / (distance * distance);
    }
    public static NetForce netForce(Planet planet, Square node) {
        if (node == null)
            return new NetForce(0, 0); // node null, no children aka no gravity
        
        double distance = utils.calculateDistance(planet.getXY(), node.centerMass);
        double gravity;
        NetForce ret = new NetForce(0, 0);
        boolean isSufficientlyFar = distance > planet.getSquare().size / 2.0; // tetragwno ara pleura size/4
        boolean hasPlanet = utils.nodeContainsPlanet(planet, node);
        if (isSufficientlyFar && !hasPlanet) {
            // calculate the gravity of all bodies in the node
            // center of mass being the node's center of mass
            gravity = utils.calculateGravity(planet.getMass(), node.mass, distance);
            ret.fx = (gravity * (node.xy.x - planet.getX())) / distance;
            ret.fy = (gravity * (node.xy.y - planet.getY())) / distance;
        } else if (!isSufficientlyFar || hasPlanet) {
            // recursively calculate gravity the children exert at the body
            if (node.ne != null)
                ret = ret.add(netForce(planet, node.ne));
            if (node.nw != null)
                ret = ret.add(netForce(planet, node.nw));
            if (node.se != null)
                ret = ret.add(netForce(planet, node.se));
            if (node.sw != null)
                ret = ret.add(netForce(planet, node.sw));
            // sunhstamenh twn dynamewn einai to a8roisma twn dunamewn
        }
        return ret;
    }
    
}

class Planet {
    private CoordinatesXY xy;
    private double velocityX;
    private double velocityY;
    private double mass;
    private boolean exploded;
    private String name;
    private Square square; // square the leaf it belongs

    // Constructor
    public Planet(CoordinatesXY xy, double velocityX, double velocityY, double mass, String name) {
        this.xy = xy;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.mass = mass;
        this.name = name;
        this.exploded = false;
    }

    // Default constructor
    public Planet() {
        this.xy = new CoordinatesXY(0, 0);
        this.velocityX = 0;
        this.velocityY = 0;
        this.mass = 0;
        this.name = "";
        this.exploded = false;
    }

    // Getter functions
    public CoordinatesXY getXY() {
        return xy;
    }

    public double getX() {
        return xy.x;
    }

    public double getY() {
        return xy.y;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getMass() {
        return mass;
    }

    public boolean hasExploded() {
        return exploded;
    }

    public String getName() {
        return name;
    }

    public Square getSquare() {
        return square;
    }

    // Setter functions
    public void setXY(CoordinatesXY xy) {
        this.xy = xy;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSquare(Square square) {
        this.square = square;
    }

    // Other
    public void initiateExplosion() {
        exploded = true;
    }

    public void gotoNextPosition() {
        if (exploded) return;
        final double dt = 1;
        double vx = xy.x + dt * velocityX;
        double vy = xy.y + dt * velocityY;
        xy = new CoordinatesXY(xy.x + dt * vx, xy.y + dt * vy);
    }
}


class Square {
    public double size; // R*2
    public long id;
    public CoordinatesXY xy;
    public CoordinatesXY centerMass;
    public double mass;
    public List<Planet> planets;
    public List<Long> planetIndexes; // ideally pointer
    public long level;
    public Square nw, ne, sw, se, parent;

    public CoordinatesXY calculateCenterMass(Square sq1, Square sq2) {
        double x = 0, y = 0;
        double mass = sq1.mass + sq2.mass;
        if (mass <= 0) {
            return sq1.centerMass;
        }
        x = (sq1.centerMass.x * sq1.mass + sq2.centerMass.x * sq2.mass) / mass;
        y = (sq1.centerMass.y * sq1.mass + sq2.centerMass.y * sq2.mass) / mass;
        return new CoordinatesXY(x, y);
    }

    public boolean planetInSquare(CoordinatesXY planetXY, CoordinatesXY squareXY, double squareSize) {
        return planetXY.x <= squareXY.x + squareSize / 2.0 &&
               planetXY.x >= squareXY.x - squareSize / 2.0 &&
               planetXY.y <= squareXY.y + squareSize / 2.0 &&
               planetXY.y >= squareXY.y - squareSize / 2.0;
    }

    public void buildTree() {
        mass = 0;
        centerMass = xy;
        if (planetIndexes.size() < 2) {
            if (!planetIndexes.isEmpty()) {
                planets.get(planetIndexes.get(0).intValue()).setSquare(this);
                mass = planets.get(planetIndexes.get(0).intValue()).getMass();
            }
            return;
        }
        List<Long> nwPlanets = new ArrayList<>();
        List<Long> nePlanets = new ArrayList<>();
        List<Long> swPlanets = new ArrayList<>();
        List<Long> sePlanets = new ArrayList<>();
        CoordinatesXY nwXY = new CoordinatesXY(-size / 4.0 + xy.x, size / 4.0 + xy.y);
        CoordinatesXY neXY = new CoordinatesXY(size / 4.0 + xy.x, size / 4.0 + xy.y);
        CoordinatesXY swXY = new CoordinatesXY(-size / 4.0 + xy.x, -size / 4.0 + xy.y);
        CoordinatesXY seXY = new CoordinatesXY(size / 4.0 + xy.x, -size / 4.0 + xy.y);
        for (long index : planetIndexes) {
            if (planetInSquare(planets.get((int) index).getXY(), nwXY, size / 2.0)) {
                nwPlanets.add(index);
            } else if (planetInSquare(planets.get((int) index).getXY(), neXY, size / 2.0)) {
                nePlanets.add(index);
            } else if (planetInSquare(planets.get((int) index).getXY(), swXY, size / 2.0)) {
                swPlanets.add(index);
            } else if (planetInSquare(planets.get((int) index).getXY(), seXY, size / 2.0)) {
                sePlanets.add(index);
            } else {
                planets.get((int) index).initiateExplosion();
            }
        }

        nw = new Square(planets, nwXY, size / 2.0, nwPlanets, level + 1, this);
        centerMass = calculateCenterMass(this, nw);
        mass += nw.mass;

        ne = new Square(planets, neXY, size / 2.0, nePlanets, level + 1, this);
        centerMass = calculateCenterMass(this, ne);
        mass += ne.mass;

        sw = new Square(planets, swXY, size / 2.0, swPlanets, level + 1, this);
        centerMass = calculateCenterMass(this, sw);
        mass += sw.mass;

        se = new Square(planets, seXY, size / 2.0, sePlanets, level + 1, this);
        centerMass = calculateCenterMass(this, se);
        mass += se.mass;
    }

    public Square(List<Planet> planets, CoordinatesXY xy, double size) {
        this.planets = planets;
        this.xy = xy;
        this.size = size;
        this.level = 0;
        this.parent = null;
        this.centerMass = new CoordinatesXY(0, 0);
        this.mass = 0;
        this.planetIndexes = new ArrayList<>();
        for (int i = 0; i < planets.size(); i++) {
            planetIndexes.add((long) i);
        }
        nw = null;
        ne = null;
        se = null;
        sw = null;
        buildTree();
    }

    public Square(List<Planet> planets, CoordinatesXY xy, double size, List<Long> planetIndexes, long level, Square parent) {
        this.planets = planets;
        this.xy = xy;
        this.size = size;
        this.level = level;
        this.parent = parent;
        this.centerMass = new CoordinatesXY(0, 0);
        this.mass = 0;
        this.planetIndexes = new ArrayList<>(planetIndexes);
        nw = null;
        ne = null;
        se = null;
        sw = null;
        buildTree();
    }
}


class NetForce {
    public double fx;
    public double fy;

    public NetForce(double fx, double fy) {
        this.fx = fx;
        this.fy = fy;
    }

    public NetForce() {
        this.fx = 0;
        this.fy = 0;
    }

    public NetForce add(NetForce other) {
        this.fx += other.fx;
        this.fy += other.fy;
        return this;
    }
}

class NetForceThread extends Thread {
    private List<Planet> planets;
    private Square universe;
    private int start;
    private int end;

    public NetForceThread(List<Planet> planets, Square universe, int start, int end) {
        this.planets = planets;
        this.universe = universe;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        for (int i = start; i < end; i++) {
            NetForce force = utils.netForce(planets.get(i), universe);
            planets.get(i).setVelocityX(force.fx / planets.get(i).getMass());
            planets.get(i).setVelocityY(force.fy / planets.get(i).getMass());
        }
    }
}

class GoNextTask extends Thread{
    private List<Planet> planets;
    private int start;
    private int end;

    public GoNextTask(List<Planet> planets, int start, int end){
        this.planets=planets;
        this.start=start;
        this.end=end;
    }
    @Override
    public void run(){
        for(int i=start; i<end; i++){
            planets.get(i).gotoNextPosition();
        }
    }
}

class main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java Main <filename> <iterations_num> <thread_num>");
            System.exit(1);
        }
        long iterations = Long.parseLong(args[1]); // 'seconds' the simulation will run
        int threadNum = Integer.parseInt(args[2]);
        double ellapsed=0;
        List<Planet> planets = new ArrayList<>();
        double numPlanets=0;
        double universeSize=0;
        double R; // radius of the universe
        double x, y, velocityX, velocityY, mass;
        String name;
        System.out.println("total threads: "+threadNum);
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(args[0]));        
            String[] line = fileReader.readLine().split(" ");
            numPlanets = Double.parseDouble(line[0]);
            line = fileReader.readLine().split(" ");
            R = Double.parseDouble(line[0]);
            universeSize = R * 2;
            for (int i = 0; i < numPlanets; i++) {
                line = fileReader.readLine().split(" ");
                x = Double.parseDouble(line[0]);
                y = Double.parseDouble(line[1]);
                velocityX = Double.parseDouble(line[2]);
                velocityY = Double.parseDouble(line[3]);
                mass = Double.parseDouble(line[4]);
                name = line[5];
                Planet planet = new Planet(new CoordinatesXY(x, y), velocityX, velocityY, mass, name);
                planets.add(planet);
            }
            
            // for (Planet planet : planets) {
            //     System.out.println("Name: " + planet.getName() + ", " +
            //                        "Position: (" + planet.getX() + ", " + planet.getY() + "), " +
            //                        "Velocity: (" + planet.getVelocityX() + ", " + planet.getVelocityY() + "), " +
            //                        "Mass: " + planet.getMass());
            // }
            fileReader.close();
        } catch (IOException e) {
            System.err.println("Failed to open file " + args[0]);
            e.printStackTrace();
            System.exit(1);
        }
        // Time to do the parallel thing
        Instant start,end; 
        Duration duration;
        int partition = planets.size() / threadNum;
        for(int i =0; i<iterations; i++){
            Square universe = new Square(planets, new CoordinatesXY(0, 0), universeSize);
            start = Instant.now();
            List<Thread> threads = new ArrayList<>();
            List<Thread> threadsNext = new ArrayList<>();
            ///======================================================
            for (int j = 0; j <threadNum; j++){
                int startIndex = j * partition;
                int endIndex = (i==threadNum -1)? planets.size():(j+1)*partition;
                Thread thread = new NetForceThread(planets, universe, startIndex, endIndex);
                threads.add(thread);
                thread.start();
            }
            for(Thread thread: threads){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            ///======================================================

            for (int j = 0; j <threadNum; j++){
                int startIndex = j * partition;
                int endIndex = (i==threadNum -1)? planets.size():(j+1)*partition;
                Thread thread = new GoNextTask(planets, startIndex, endIndex);
                threadsNext.add(thread);
                thread.start();
            }
            for(Thread thread: threadsNext){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            ///======================================================

            end = Instant.now();

            duration = Duration.between(start, end);
            ellapsed += duration.getSeconds() + duration.getNano() / 1_000_000_000.0;

        }

        try (BufferedWriter output = new BufferedWriter(new FileWriter("output.txt"))) {
            output.write(numPlanets + "\n");
            output.write(universeSize + "\n");
            for (Planet planet : planets) {
                output.write(planet.getX() + " " + planet.getY() + " " + planet.getVelocityX() + " " + planet.getVelocityY() + " " + planet.getMass() + " " + planet.getName() + "\n");
            }
            System.out.println("NetForce time calculation " + ellapsed + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write to file output.txt");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

