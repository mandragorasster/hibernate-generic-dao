package com.test.junit.hibernate;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;

import com.test.base.TestBase;
import com.test.misc.HibernateBaseDAOTester;
import com.test.model.Home;
import com.test.model.Ingredient;
import com.test.model.Person;
import com.test.model.Pet;
import com.test.model.Recipe;
import com.test.model.RecipeIngredient;
import com.test.model.RecipeIngredientId;
import com.test.model.Store;
import com.trg.dao.search.Fetch;
import com.trg.dao.search.Search;

public class BaseDAOTest extends TestBase {

	private HibernateBaseDAOTester target;

	public void setHibernateBaseDAOTester(HibernateBaseDAOTester dao) {
		this.target = dao;
	}

	public void testSave() {
		Serializable id = null;

		id = target.save(grandpaA.getHome().getAddress());
		assertEquals("returned ID should match assigned ID", grandpaA.getHome().getAddress().getId(), id);
		id = target.save(grandpaA.getHome());
		assertEquals("returned ID should match assigned ID", grandpaA.getHome().getId(), id);
		id = target.save(grandpaA);
		assertEquals("returned ID should match assigned ID", grandpaA.getId(), id);

		List<Person> list = target.all(Person.class);
		assertEquals(1, list.size());
		assertEquals(grandpaA, list.get(0));

		assertEquals(grandpaA, target.get(Person.class, grandpaA.getId()));

		target.save(papaA.getHome().getAddress());
		target.save(papaA.getHome());
		target.save(grandmaA);
		target.save(papaA);
		target.save(mamaA);
		target.save(joeA);

		grandpaA.setFirstName("Dean");

		assertEquals("Dean", target.get(Person.class, joeA.getId()).getFather().getFather().getFirstName());

		grandpaA.setFirstName("Grandpa");
	}

	public void testUpdate() {
		initDB();
		Person fred = copy(papaA);
		fred.setFirstName("Fred");
		target.update(fred);

		assertTrue(target.sessionContains(fred));

		assertEquals("The change should be made.", "Fred", target.get(Person.class, joeA.getId()).getFather()
				.getFirstName());

		fred.setLastName("Santos");
		Search s = new Search(Person.class);
		s.addFetch("father.lastName");
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFilterEqual("id", joeA.getId());

		assertEquals("The change should be made.", "Santos", target.searchUnique(s));

		Person otherFred = copy(papaA);
		try {
			target.update(otherFred);
			fail("Should throw exception when there is a persistent instance with the same identifier.");
		} catch (HibernateException e) {
		}
	}

	public void testPersist() {
		clearIds();

		target.persist(grandpaA.getHome().getAddress());
		target.persist(grandpaA.getHome());
		target.persist(grandpaA);

		List<Person> list = target.all(Person.class);
		assertEquals(1, list.size());
		assertEquals(grandpaA, list.get(0));

		assertEquals(grandpaA, target.load(Person.class, grandpaA.getId()));

		target.persist(papaA.getHome().getAddress());
		target.persist(papaA.getHome());
		target.persist(grandmaA);
		target.persist(papaA);
		target.persist(mamaA);
		target.persist(joeA);

		grandpaA.setFirstName("Dean");

		assertEquals("Dean", target.load(Person.class, joeA.getId()).getFather().getFather().getFirstName());

		grandpaA.setFirstName("Grandpa");
	}

	public void testMerge() {
		initDB();
		Person fred = copy(papaA);
		fred.setFirstName("Fred");
		Person attachedFred = target.merge(fred);

		assertEquals("The change should be made.", "Fred", target.get(Person.class, joeA.getId()).getFather()
				.getFirstName());

		assertFalse(target.sessionContains(fred));
		assertTrue(target.sessionContains(attachedFred));

		Search s = new Search(Person.class);
		s.addFetch("father.lastName");
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFilterEqual("id", joeA.getId());

		fred.setLastName("Santos");
		assertEquals("The change should not be made.", "Alpha", target.searchUnique(s));

		attachedFred.setLastName("Santos");
		assertEquals("The change should be made.", "Santos", target.searchUnique(s));
	}

	public void testDelete() {
		initDB();

		List<Person> list = target.all(Person.class);
		int sizeBefore = list.size();

		assertTrue("Should return true when successfully deleting", target.deleteById(Person.class, joeA.getId()));
		assertTrue("Should return true when successfully deleting", target.deleteEntity(sallyA));

		list = target.all(Person.class);
		assertEquals(sizeBefore - 2, list.size());
		for (Person person : list) {
			if (person.getId().equals(joeA.getId()) || person.getId().equals(sallyA.getId()))
				fail("Neither Joe nor Sally should now be in the DB");
		}

		target.save(joeA);
		target.save(sallyA);

		list = target.all(Person.class);
		assertEquals(sizeBefore, list.size());
		boolean joeFound = false, sallyFound = false;
		for (Person person : list) {
			if (person.getFirstName().equals("Joe") && person.getLastName().equals("Alpha"))
				joeFound = true;
			if (person.getFirstName().equals("Sally") && person.getLastName().equals("Alpha"))
				sallyFound = true;
		}
		assertTrue("Joe and Sally should now be back in the DB", joeFound && sallyFound);

		// Test deleting by non-existent ID.
		Search s = new Search(Person.class);
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFetch("id", Fetch.OP_MAX);
		Long unusedId = ((Long) target.searchUnique(s)).longValue() + 1;

		// deleteById should not throw an error
		assertFalse(target.deleteById(Person.class, unusedId));

		Person fake = new Person();
		assertFalse("return false when no ID", target.deleteEntity(fake));
		fake.setId(unusedId);
		assertFalse("return false when ID not found", target.deleteEntity(fake));
	}

	public void testLoad() {
		initDB();

		Person joe = new Person();
		target.load(joe, joeA.getId());
		assertEquals(joe.getId(), joeA.getId());
		assertEquals(joe.getAge(), joeA.getAge());
	}

	@SuppressWarnings("unchecked")
	public void testForceClass() {
		Person bob = copy(grandpaA);
		Person fred = copy(grandmaA);
		target.save(bob);
		target.save(fred);

		Search s = new Search();
		Search sP = new Search(Person.class);
		Search sH = new Search(Home.class);
		// search
		assertListEqual(new Person[] { bob, fred }, target.search(s, Person.class));
		assertListEqual(new Person[] { bob, fred }, target.search(sP, Person.class));
		assertEquals(null, s.getSearchClass());
		assertEquals(Person.class, sP.getSearchClass());

		// count
		assertEquals(2, target.count(s, Person.class));
		assertEquals(2, target.count(sP, Person.class));
		assertEquals(null, s.getSearchClass());
		assertEquals(Person.class, sP.getSearchClass());

		// searchAndCount
		assertListEqual(new Person[] { bob, fred }, target.searchAndCount(s, Person.class).results);
		assertListEqual(new Person[] { bob, fred }, target.searchAndCount(sP, Person.class).results);
		assertEquals(null, s.getSearchClass());
		assertEquals(Person.class, sP.getSearchClass());

		// searchUnique
		s.addFilterEqual("id", bob.getId());
		assertEquals(bob, target.searchUnique(s, Person.class));
		sP.addFilterEqual("id", bob.getId());
		assertEquals(bob, target.searchUnique(sP, Person.class));
		assertEquals(null, s.getSearchClass());
		assertEquals(Person.class, sP.getSearchClass());

		try {
			target.search(sH, Person.class);
			fail("An error should be thrown when a different class is specified in the Search.");
		} catch (IllegalArgumentException ex) {
			assertEquals(Home.class, sH.getSearchClass());
		}
		try {
			target.count(sH, Person.class);
			fail("An error should be thrown when a different class is specified in the Search.");
		} catch (IllegalArgumentException ex) {
			assertEquals(Home.class, sH.getSearchClass());
		}
		try {
			target.searchAndCount(sH, Person.class);
			fail("An error should be thrown when a different class is specified in the Search.");
		} catch (IllegalArgumentException ex) {
			assertEquals(Home.class, sH.getSearchClass());
		}
		try {
			target.searchUnique(sH, Person.class);
			fail("An error should be thrown when a different class is specified in the Search.");
		} catch (IllegalArgumentException ex) {
			assertEquals(Home.class, sH.getSearchClass());
		}
	}

	public void testSaveMulti() {
		Serializable id = null;

		target.save(grandpaA.getHome().getAddress(), grandpaA.getHome(), grandpaA);

		List<Person> list = target.all(Person.class);
		assertEquals(1, list.size());
		assertEquals(grandpaA, list.get(0));

		assertEquals(grandpaA, target.get(Person.class, grandpaA.getId()));

		target.save(papaA.getHome().getAddress(), papaA.getHome(), grandmaA, papaA, mamaA, joeA);

		grandpaA.setFirstName("Dean");
		try {
			assertEquals("Dean", target.get(Person.class, joeA.getId()).getFather().getFather().getFirstName());
		} finally {
			grandpaA.setFirstName("Grandpa");
		}
	}

	public void testSaveOrUpdate() {

		initDB();

		String[] orig = new String[] { grandpaA.getFirstName(), grandmaA.getFirstName() };

		try {
			grandpaA.setFirstName("GGG1");
			grandmaA.setFirstName("GGG2");

			// update
			boolean[] isNew = target.saveOrUpdateIsNew(grandpaA.getHome().getAddress(), grandpaA.getHome(), grandpaA);
			assertFalse(isNew[0]);
			assertFalse(isNew[1]);
			assertFalse(isNew[2]);

			target.saveOrUpdate(grandmaA);

			assertFalse(target.saveOrUpdateIsNew(papaA));

			Person bob = new Person("Bob", "Loblaw");

			// save with null id
			assertTrue(target.saveOrUpdateIsNew(bob));

			Person[] people = new Person[] { new Person("First", "Person"), new Person("Second", "Person") };

			// save with null id (multi)
			isNew = target.saveOrUpdateIsNew((Object[]) people);
			assertTrue(isNew[0]);
			assertTrue(isNew[1]);

			Search s = new Search(Person.class);
			s.addFilterIn("firstName", "GGG1", "GGG2", "Bob", "First", "Second");
			assertListEqual(new Person[] { grandpaA, grandmaA, bob, people[0], people[1] }, target.search(s));

			grandpaA.setFirstName("GGG3");
			grandmaA.setFirstName("GGG4");
			bob.setFirstName("Bobby");
			people[0].setFirstName("Firstly");
			people[1].setFirstName("Secondly");

			s.clear();
			s.addFilterIn("firstName", "GGG3", "GGG4", "Bobby", "Firstly", "Secondly");
			assertListEqual(new Person[] { grandpaA, grandmaA, bob, people[0], people[1] }, target.search(s));

			// save with non-null id
			s.clear();
			s.setFetchMode(Search.FETCH_SINGLE);
			s.addFetch("id", Fetch.OP_MAX);
			long maxPersonId = (Long) target.searchUnique(s);

			Person sam = new Person("Sam", "Wodsworth");
			sam.setId(++maxPersonId);
			assertTrue(target.saveOrUpdateIsNew(sam));

			assertTrue(target.sessionContains(sam));

			s.clear();
			s.addFilterEqual("firstName", "Sam");
			assertEquals(sam, target.searchUnique(s));

			// save with non-null id (multi)
			people = new Person[] { new Person("Arty", "Millstone"), new Person("Pascal", "Rudington") };
			people[0].setId(++maxPersonId);
			people[1].setId(++maxPersonId);

			isNew = target.saveOrUpdateIsNew(people[0], people[1]);
			assertTrue(isNew[0]);
			assertTrue(isNew[1]);

			s.clear();
			s.addFilterIn("firstName", "Arty", "Pascal");
			assertEquals(2, target.search(s).size());

			// save some update some, also multiple types
			long maxPetId = (Long) target.searchUnique(new Search(Pet.class).setFetchMode(Search.FETCH_SINGLE)
					.addFetch("id", Fetch.OP_MAX));

			people[1] = new Person("Miley", "Gordon");
			people[1].setId(++maxPersonId);

			Pet[] pets = new Pet[4];
			pets[0] = catPrissy;
			pets[1] = target.get(Pet.class, catNorman.getId());
			pets[2] = new Pet("dog", "Mr.", "Waddlesworth", 42);
			pets[2].setId(++maxPetId);
			pets[3] = new Pet("dog", "Mrs.", "Waddlesworth", 43);

			isNew = target.saveOrUpdateIsNew(people[0], pets[0], pets[1], people[1], pets[2], pets[3]);
			assertFalse(isNew[0]);
			assertFalse(isNew[1]);
			assertFalse(isNew[2]);
			assertTrue(isNew[3]);
			assertTrue(isNew[4]);
			assertTrue(isNew[5]);

			assertTrue(target.sessionContains(people[0]));
			assertTrue(target.sessionContains(people[1]));
			assertTrue(target.sessionContains(pets[0]));
			assertTrue(target.sessionContains(pets[1]));
			assertTrue(target.sessionContains(pets[2]));
			assertTrue(target.sessionContains(pets[3]));

		} finally {
			grandpaA.setFirstName(orig[0]);
			grandmaA.setFirstName(orig[1]);
		}
	}

	public void testGetLoadMulti() {
		initDB();

		Search s = new Search(Person.class);
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFetch("id", Fetch.OP_MAX);
		long maxId = (Long) target.searchUnique(s);

		Person[] people = target.get(Person.class, papaA.getId(), maxId + 1, papaB.getId());
		assertEquals(3, people.length);
		assertEquals(papaA.getId(), people[0].getId());
		assertEquals(papaA.getAge(), people[0].getAge());
		assertNull(people[1]);
		assertEquals(papaB.getId(), people[2].getId());
		assertEquals(papaB.getAge(), people[2].getAge());

		people = target.load(Person.class, mamaA.getId(), maxId + 1, mamaB.getId());
		assertEquals(3, people.length);
		assertEquals(mamaA.getId(), people[0].getId());
		assertEquals(mamaA.getAge(), people[0].getAge());
		assertEquals(mamaB.getId(), people[2].getId());
		assertEquals(mamaB.getAge(), people[2].getAge());

		try {
			people[1].getAge();
			fail("Entity does not exist, should throw error.");
		} catch (ObjectNotFoundException ex) {
		}

	}

	public void testDeleteMulti() {
		initDB();

		Search s = new Search(Person.class);
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFetch("id", Fetch.OP_MAX);
		long maxId = (Long) target.searchUnique(s);

		target.update(papaA);
		target.update(papaB);
		target.update(mamaA);
		target.update(mamaB);

		// delete unattached
		assertFalse(target.sessionContains(joeA));
		assertFalse(target.sessionContains(joeB));
		assertFalse(target.sessionContains(sallyA));
		assertFalse(target.sessionContains(margretB));

		target.deleteById(Person.class, joeA.getId(), null, joeB.getId(), maxId + 1);

		assertNull(target.get(Person.class, joeA.getId()));
		assertFalse(target.sessionContains(joeA));
		assertNull(target.get(Person.class, joeB.getId()));
		assertFalse(target.sessionContains(joeB));

		target.deleteEntities(sallyA, null, margretB, catNorman);

		assertNull(target.get(Person.class, sallyA.getId()));
		assertFalse(target.sessionContains(sallyA));
		assertNull(target.get(Person.class, margretB.getId()));
		assertFalse(target.sessionContains(margretB));
		assertNull(target.get(Pet.class, catNorman.getId()));
		assertFalse(target.sessionContains(catNorman));

		// delete attached
		assertTrue(target.sessionContains(papaA));
		assertTrue(target.sessionContains(papaB));
		assertTrue(target.sessionContains(mamaA));
		assertTrue(target.sessionContains(mamaB));

		target.deleteById(Person.class, papaA.getId(), null, papaB.getId(), maxId + 1);

		assertNull(target.get(Person.class, papaA.getId()));
		assertFalse(target.sessionContains(papaA));
		assertNull(target.get(Person.class, papaB.getId()));
		assertFalse(target.sessionContains(papaB));

		target.deleteEntities(mamaA, mamaB);

		assertNull(target.get(Person.class, mamaA.getId()));
		assertFalse(target.sessionContains(mamaA));
		assertNull(target.get(Person.class, mamaB.getId()));
		assertFalse(target.sessionContains(mamaB));

	}

	public void testExists() {
		initDB();

		Search s = new Search(Store.class);
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFetch("id", Fetch.OP_MAX);
		long maxStoreId = (Long) target.searchUnique(s);

		s.setSearchClass(Recipe.class);
		long maxRecipeId = (Long) target.searchUnique(s);

		s.setSearchClass(Ingredient.class);
		long maxIngredientId = (Long) target.searchUnique(s);

		assertTrue(target.exists(Store.class, maxStoreId));
		assertFalse(target.exists(Store.class, maxStoreId + 1));

		Store store = target.get(Store.class, maxStoreId);
		assertTrue(target.exists(Store.class, maxStoreId));
		assertTrue(target.exists(store));

		boolean[] exists = target.exists(Store.class, maxStoreId, maxStoreId + 1);
		assertTrue(exists[0]);
		assertFalse(exists[1]);

		exists = target.exists(Recipe.class, maxRecipeId, maxRecipeId + 1);
		assertTrue(exists[0]);
		assertFalse(exists[1]);

		store = new Store();
		assertFalse(target.exists(store)); // id = 0

		store.setId(maxStoreId + 1);
		assertFalse(target.exists(store));

		store.setId(maxStoreId);
		assertTrue(target.exists(store));

		s.clear();
		s.setSearchClass(Ingredient.class);
		s.addSort("name");
		List<Ingredient> ingredients = target.search(s);
		// Butter, Chicken, Flour, Salt, Sugar, Yeast

		// recipes:
		// Bread, Fried Chicken, Toffee

		assertTrue(target.exists(RecipeIngredient.class, recipes.get(0).getIngredients().iterator().next().getId()));
		assertTrue(target.exists(RecipeIngredient.class, new RecipeIngredientId(recipes.get(1), ingredients.get(1))));
		assertFalse(target.exists(RecipeIngredient.class, new RecipeIngredientId(recipes.get(1), ingredients.get(5))));

		assertTrue(target.exists(recipes.get(0).getIngredients().iterator().next()));

		RecipeIngredient ri = new RecipeIngredient();
		assertFalse(target.exists(ri));

		ri.setId(new RecipeIngredientId(recipes.get(1), ingredients.get(5)));
		assertFalse(target.exists(ri));

		ri.setId(new RecipeIngredientId(recipes.get(1), ingredients.get(1)));
		assertTrue(target.exists(ri));
	}

	public void testCompoundId() {
		initDB();
		
		Search s = new Search(Recipe.class);
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFetch("id", Fetch.OP_MAX);
		long maxRecipeId = (Long) target.searchUnique(s);

		s.setSearchClass(Ingredient.class);
		long maxIngredientId = (Long) target.searchUnique(s);

		s.clear();
		s.addSort("name");
		List<Ingredient> ingredients = target.search(s);
		// Butter, Chicken, Flour, Salt, Sugar, Yeast

		// recipes:
		// Bread, Fried Chicken, Toffee		
		
		
		//save & update
		RecipeIngredient ri = new RecipeIngredient(recipes.get(0), ingredients.get(0), .125f, "cup"); //new
		RecipeIngredient ri2 = new RecipeIngredient(recipes.get(0), ingredients.get(1), .25f, "lb."); //new
		RecipeIngredient ri3 = new RecipeIngredient(recipes.get(2), ingredients.get(4), 2.25f, "cups"); //not new
		RecipeIngredient ri4 = new RecipeIngredient(recipes.get(1), ingredients.get(1), 3f, "lbs."); //not new
		
		assertTrue(target.saveOrUpdateIsNew(ri));
		
		boolean[] isNew = target.saveOrUpdateIsNew(ri2, ri3);
		assertTrue(isNew[0]);
		assertFalse(isNew[1]);
		
		assertFalse(target.saveOrUpdateIsNew(ri4));
		
		//get
		assertEquals(ri, target.get(RecipeIngredient.class, ri.getId()));
		assertEquals(ri2, target.get(RecipeIngredient.class, new RecipeIngredientId(recipes.get(0), ingredients.get(1))));
		
		Recipe r = new Recipe();
		r.setId(recipes.get(0).getId());
		Ingredient i = new Ingredient();
		i.setId(ingredients.get(0).getId());
		
		assertEquals(ri, target.get(RecipeIngredient.class, new RecipeIngredientId(r, i)));
		
		//search
		s.clear();
		s.setSearchClass(RecipeIngredient.class);
		s.setFetchMode(Search.FETCH_SINGLE);
		s.addFetch("id");
		s.addSort("id");
		s.addFilterEqual("id", ri.getId());
		
		assertEquals(ri.getId(), target.searchUnique(s));
		
		//exists (see exists test)
	}
}
