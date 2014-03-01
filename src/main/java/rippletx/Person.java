package rippletx;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class Person {

    @GraphId Long id;
    public String name;

    public Person() {}
    public Person(String name) { this.name = name; }

    @RelatedTo(type="TRANSACTS_WITH", direction=Direction.BOTH)
    public @Fetch Set<Person> transactors;

    public void transactsWith(Person person) {
        if (transactors == null) {
            transactors = new HashSet<Person>();
        }
        transactors.add(person);
    }

    public String toString() {
        String results = name + " transacts with\n";
        if (transactors != null) {
            for (Person person : transactors) {
                results += "\t- " + person.name + "\n";
            }
        }
        return results;
    }

}
