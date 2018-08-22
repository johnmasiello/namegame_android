package com.willowtreeapps.namegame.core.gameLogic;

import android.support.test.filters.SmallTest;

import com.willowtreeapps.namegame.core.gamelogic.ListRandomizer;
import com.willowtreeapps.namegame.network.api.model.Person;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.mock;

@SmallTest
public class ListRandomizerTest {
    private List<Person> people;

    @Before
    public void setUp() {
        people = new ArrayList<>();
        people.add(new Person("1", null, null, null, "Bill", "Smith", null, null));
        people.add(new Person("2", null, null, null, "Pam", "White", null, null));
        people.add(new Person("3", null, null, null, "Fred", "Doe", null, null));
        people.add(new Person("4", null, null, null, "Jeff", "Ward", null, null));
        people.add(new Person("5", null, null, null, "Ashley", "Joost", null, null));
        people.add(new Person("6", null, null, null, "Ben", "Frye", null, null));
    }

    @Test
    public void testPickOne() {
        // Mock the dependencies
        Random random = mock(Random.class);
        // Create the test
        ListRandomizer listRandomizer = new ListRandomizer(random);
        Person person;

        for (int i = 0; i < 10; i++) {
            person = listRandomizer.pickOne(people);
            Assert.assertTrue("List does not contain the selected person with id="+
                            person.getId(),
                    containsPerson(people, person.getId()));
        }
    }

    private boolean containsPerson(List<Person> people, String id) {
        for (Person person : people)
            if (person.getId().equals(id))
                return true;
        return false;
    }

    @Test
    public void testPickThreeNoFilter() {
        // Mock the dependencies
        Random random = mock(Random.class);
        // Create the test
        ListRandomizer listRandomizer = new ListRandomizer(random);
        Assert.assertEquals("The sublist of people should be of size three",
                3,
                listRandomizer.pickN(people, 3).size());
    }

    @Test
    public void testPickPeopleContainingAn_A_InTheirName() {
        // Mock the dependencies
        Random random = mock(Random.class);
        // Create the test
        ListRandomizer listRandomizer = new ListRandomizer(random);
        List<Person> selected = listRandomizer.pickN(people, 6, new ListRandomizer
                .ListFilter<Person>() {
            @Override
            public boolean accept(Person item) {
                return item.getFirstName().toLowerCase().contains("a") ||
                        item.getLastName().toLowerCase().contains("a");
            }
        });
        Assert.assertTrue("Selected People should contain Pam White",
                containsPerson(selected, "2"));
        Assert.assertTrue("Selected People should contain Jeff Ward",
                containsPerson(selected, "4"));
        Assert.assertTrue("Selected People should contain Ashley Joost",
                containsPerson(selected, "5"));

        Assert.assertEquals("Selected People should not contain more people than "+
            "the accepted set",
                    3,
                    selected.size());
    }
}
