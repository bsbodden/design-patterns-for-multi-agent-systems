#!/usr/bin/env python3
"""
Curate expanded Olist dataset for DevNexus demo.

Generates ~300-500 orders, ~50-80 sellers, ~200-400 customers, ~100-200 products
with planted entities and curated patterns (late deliveries, bad sellers,
1-star reviews, multi-issue orders, happy path, geographic clusters).

Usage:
    python code/scripts/curate-olist-data.py          # generate from synthetic expansion
    python code/scripts/curate-olist-data.py --from-raw  # use raw Kaggle CSVs if available
"""
import json, random, os, sys, string
from datetime import datetime, timedelta
from pathlib import Path

random.seed(42)  # reproducible

OUT_DIR = Path(__file__).resolve().parent.parent / "shared-data" / "olist"

# ─── Brazilian geography ───────────────────────────────────────────
CITIES = {
    "SP": [("Sao Paulo","SP"),("Campinas","SP"),("Santos","SP"),("Guarulhos","SP"),
           ("Osasco","SP"),("Ribeirao Preto","SP"),("Sorocaba","SP"),("Sao Bernardo do Campo","SP")],
    "RJ": [("Rio de Janeiro","RJ"),("Niteroi","RJ"),("Petropolis","RJ"),("Nova Iguacu","RJ")],
    "MG": [("Belo Horizonte","MG"),("Uberlandia","MG"),("Juiz de Fora","MG")],
    "PR": [("Curitiba","PR"),("Londrina","PR"),("Maringa","PR")],
    "RS": [("Porto Alegre","RS"),("Caxias do Sul","RS"),("Pelotas","RS")],
    "SC": [("Florianopolis","SC"),("Joinville","SC"),("Blumenau","SC")],
    "BA": [("Salvador","BA"),("Feira de Santana","BA")],
    "PE": [("Recife","PE"),("Olinda","PE")],
    "DF": [("Brasilia","DF")],
    "AM": [("Manaus","AM")],
    "CE": [("Fortaleza","CE")],
    "GO": [("Goiania","GO")],
    "PA": [("Belem","PA")],
    "ES": [("Vitoria","ES")],
}
ALL_CITIES = []
for cs in CITIES.values():
    ALL_CITIES.extend(cs)

CATEGORIES = [
    "electronics","health_beauty","furniture_decor","computers_accessories",
    "fashion_bags_accessories","sports_leisure","bed_bath_table","watches_gifts",
    "telephony","audio","toys","auto","garden_tools","stationery",
    "cool_stuff","housewares","food_drink","pet_shop","baby","perfumery",
]

PAYMENT_TYPES = ["credit_card","boleto","debit_card","voucher"]

REVIEW_COMMENTS_GOOD = [
    "Excellent product, arrived early!","Love it, fast delivery.","Perfect condition, exactly as described.",
    "Great quality for the price.","Arrived on time, well packaged.","Very happy with this purchase.",
    "Seller was great, product is perfect.","Fast shipping, excellent quality.",
    "Exceeded expectations!","Will buy again from this seller.",
    "Beautiful product, arrived before estimated date.","Good value for money.",
    "Product works perfectly.","Exactly what I needed, thank you!","Happy customer!",
    "Superb quality and fast delivery.","Could not be happier.","Highly recommend this seller.",
]
REVIEW_COMMENTS_BAD = [
    "Late delivery, very disappointed.","Product arrived damaged.","Does not match description at all.",
    "Terrible quality, returning for refund.","Still waiting, way past estimated delivery.",
    "Worst experience ever. Never buying again.","Seller never responded to messages.",
    "Product overheats, seems defective.","Missing parts, very frustrating.",
    "Cheap quality, not worth the price.","Arrived broken, packaging was terrible.",
    "7 days late and product was scratched.","Description was misleading, want refund.",
    "Seller ignored my complaints.","Product stopped working after 2 days.",
]
REVIEW_COMMENTS_MED = [
    "Product is ok, nothing special.","Took a while but arrived fine.","Acceptable quality.",
    "Decent product for the price.","A bit slow on delivery but product works.",
    "Good product, delivery could be faster.",
]

# ─── Planted sellers (must exist with specific characteristics) ────
PLANTED_SELLERS = [
    {"sellerId":"seller_42","city":"Sao Paulo","state":"SP","avgRating":2.3,"onTimePct":60.0,
     "totalOrders":234,"delayAvgDays":5.8,"returnRate":12.5,"revenueTotal":185400.00,
     "categories":["electronics","home_garden"]},
    {"sellerId":"seller_7","city":"Rio de Janeiro","state":"RJ","avgRating":4.8,"onTimePct":98.0,
     "totalOrders":312,"delayAvgDays":0.3,"returnRate":1.2,"revenueTotal":287600.00,
     "categories":["health_beauty","sports_leisure"]},
    {"sellerId":"seller_15","city":"Belo Horizonte","state":"MG","avgRating":3.9,"onTimePct":85.0,
     "totalOrders":156,"delayAvgDays":2.1,"returnRate":4.3,"revenueTotal":98200.00,
     "categories":["furniture_decor","home_garden"]},
    {"sellerId":"seller_23","city":"Curitiba","state":"PR","avgRating":4.2,"onTimePct":91.0,
     "totalOrders":89,"delayAvgDays":1.4,"returnRate":3.1,"revenueTotal":67500.00,
     "categories":["computers_accessories","electronics"]},
    {"sellerId":"seller_31","city":"Porto Alegre","state":"RS","avgRating":3.5,"onTimePct":78.0,
     "totalOrders":201,"delayAvgDays":3.2,"returnRate":6.8,"revenueTotal":134800.00,
     "categories":["electronics","telephony"]},
    {"sellerId":"seller_9","city":"Sao Paulo","state":"SP","avgRating":4.5,"onTimePct":95.0,
     "totalOrders":178,"delayAvgDays":0.6,"returnRate":2.0,"revenueTotal":156300.00,
     "categories":["fashion_bags_accessories","fashion_shoes"]},
    {"sellerId":"seller_18","city":"Campinas","state":"SP","avgRating":2.8,"onTimePct":65.0,
     "totalOrders":145,"delayAvgDays":4.9,"returnRate":9.7,"revenueTotal":78900.00,
     "categories":["electronics","computers_accessories"]},
    {"sellerId":"seller_5","city":"Salvador","state":"BA","avgRating":4.1,"onTimePct":88.0,
     "totalOrders":67,"delayAvgDays":1.8,"returnRate":3.5,"revenueTotal":45200.00,
     "categories":["bed_bath_table","furniture_decor"]},
    {"sellerId":"seller_11","city":"Recife","state":"PE","avgRating":3.7,"onTimePct":82.0,
     "totalOrders":93,"delayAvgDays":2.5,"returnRate":5.1,"revenueTotal":61400.00,
     "categories":["watches_gifts","cool_stuff"]},
    {"sellerId":"seller_27","city":"Florianopolis","state":"SC","avgRating":4.6,"onTimePct":96.0,
     "totalOrders":54,"delayAvgDays":0.4,"returnRate":1.5,"revenueTotal":38700.00,
     "categories":["sports_leisure","health_beauty"]},
    {"sellerId":"seller_33","city":"Brasilia","state":"DF","avgRating":3.2,"onTimePct":72.0,
     "totalOrders":112,"delayAvgDays":3.8,"returnRate":7.2,"revenueTotal":82100.00,
     "categories":["telephony","electronics"]},
    {"sellerId":"seller_44","city":"Manaus","state":"AM","avgRating":3.0,"onTimePct":68.0,
     "totalOrders":76,"delayAvgDays":4.2,"returnRate":8.3,"revenueTotal":52600.00,
     "categories":["electronics","audio"]},
]

PLANTED_CUSTOMERS = [
    {"customerId":"cust_1234","city":"Sao Paulo","state":"SP","orderCount":6,"avgReviewScore":2.1,"totalSpent":1245.80},
    {"customerId":"cust_5678","city":"Rio de Janeiro","state":"RJ","orderCount":3,"avgReviewScore":4.7,"totalSpent":890.50},
    {"customerId":"cust_9012","city":"Belo Horizonte","state":"MG","orderCount":2,"avgReviewScore":3.5,"totalSpent":320.00},
    {"customerId":"cust_3456","city":"Sao Paulo","state":"SP","orderCount":4,"avgReviewScore":4.0,"totalSpent":1580.20},
    {"customerId":"cust_7890","city":"Curitiba","state":"PR","orderCount":1,"avgReviewScore":5.0,"totalSpent":299.90},
    {"customerId":"cust_2345","city":"Porto Alegre","state":"RS","orderCount":5,"avgReviewScore":3.2,"totalSpent":2100.00},
    {"customerId":"cust_6789","city":"Salvador","state":"BA","orderCount":2,"avgReviewScore":4.5,"totalSpent":450.00},
    {"customerId":"cust_0123","city":"Campinas","state":"SP","orderCount":3,"avgReviewScore":1.7,"totalSpent":678.30},
    {"customerId":"cust_4567","city":"Recife","state":"PE","orderCount":1,"avgReviewScore":3.0,"totalSpent":189.90},
    {"customerId":"cust_8901","city":"Florianopolis","state":"SC","orderCount":2,"avgReviewScore":4.8,"totalSpent":560.00},
]

PLANTED_PRODUCTS = [
    {"productId":"prod_elec_001","category":"electronics","weightG":850,"avgReviewScore":2.9},
    {"productId":"prod_elec_002","category":"electronics","weightG":1200,"avgReviewScore":3.1},
    {"productId":"prod_elec_003","category":"electronics","weightG":450,"avgReviewScore":2.5},
    {"productId":"prod_health_001","category":"health_beauty","weightG":320,"avgReviewScore":4.6},
    {"productId":"prod_health_002","category":"health_beauty","weightG":280,"avgReviewScore":4.4},
    {"productId":"prod_furn_001","category":"furniture_decor","weightG":5400,"avgReviewScore":3.8},
    {"productId":"prod_furn_002","category":"furniture_decor","weightG":12000,"avgReviewScore":4.0},
    {"productId":"prod_comp_001","category":"computers_accessories","weightG":650,"avgReviewScore":3.4},
    {"productId":"prod_fashion_001","category":"fashion_bags_accessories","weightG":180,"avgReviewScore":4.3},
    {"productId":"prod_sport_001","category":"sports_leisure","weightG":920,"avgReviewScore":4.5},
    {"productId":"prod_bed_001","category":"bed_bath_table","weightG":2800,"avgReviewScore":4.1},
    {"productId":"prod_watch_001","category":"watches_gifts","weightG":150,"avgReviewScore":3.9},
    {"productId":"prod_phone_001","category":"telephony","weightG":200,"avgReviewScore":3.2},
    {"productId":"prod_audio_001","category":"audio","weightG":380,"avgReviewScore":3.0},
]

# ─── Known planted orders (from original data — keep them) ─────────
PLANTED_ORDERS_FILE = OUT_DIR / "curated-orders.json"

# ─── Generate additional entities ──────────────────────────────────
def rand_id(prefix, length=4):
    return prefix + ''.join(random.choices(string.ascii_uppercase + string.digits, k=length))

def rand_date(start_str="2017-07-01", end_str="2018-06-30"):
    s = datetime.fromisoformat(start_str)
    e = datetime.fromisoformat(end_str)
    delta = (e - s).days
    d = s + timedelta(days=random.randint(0, delta))
    d = d.replace(hour=random.randint(6,22), minute=random.randint(0,59))
    return d

def fmt_dt(d):
    return d.strftime("%Y-%m-%dT%H:%M:%S") if d else None

def generate_sellers(planted, target=65):
    sellers = list(planted)
    existing_ids = {s["sellerId"] for s in sellers}
    i = 50
    while len(sellers) < target:
        sid = f"seller_{i}"
        if sid in existing_ids:
            i += 1
            continue
        city, state = random.choice(ALL_CITIES)
        # Mix of good and bad sellers
        tier = random.choices(["great","good","average","poor","bad"], weights=[15,30,25,20,10])[0]
        if tier == "great":
            rating = round(random.uniform(4.4, 5.0), 1)
            on_time = round(random.uniform(93, 99), 1)
            delay = round(random.uniform(0.1, 0.8), 1)
            ret_rate = round(random.uniform(0.5, 2.5), 1)
        elif tier == "good":
            rating = round(random.uniform(3.8, 4.5), 1)
            on_time = round(random.uniform(85, 95), 1)
            delay = round(random.uniform(0.5, 2.0), 1)
            ret_rate = round(random.uniform(1.5, 4.0), 1)
        elif tier == "average":
            rating = round(random.uniform(3.2, 4.0), 1)
            on_time = round(random.uniform(75, 88), 1)
            delay = round(random.uniform(1.5, 3.5), 1)
            ret_rate = round(random.uniform(3.0, 6.0), 1)
        elif tier == "poor":
            rating = round(random.uniform(2.5, 3.3), 1)
            on_time = round(random.uniform(60, 78), 1)
            delay = round(random.uniform(3.0, 5.5), 1)
            ret_rate = round(random.uniform(5.0, 10.0), 1)
        else:  # bad
            rating = round(random.uniform(1.5, 2.8), 1)
            on_time = round(random.uniform(45, 65), 1)
            delay = round(random.uniform(4.0, 8.0), 1)
            ret_rate = round(random.uniform(8.0, 18.0), 1)
        total_orders = random.randint(15, 300)
        cats = random.sample(CATEGORIES, k=random.randint(1,3))
        sellers.append({
            "sellerId": sid,
            "city": city, "state": state,
            "avgRating": rating, "onTimePct": on_time,
            "totalOrders": total_orders,
            "delayAvgDays": delay, "returnRate": ret_rate,
            "revenueTotal": round(random.uniform(10000, 350000), 2),
            "categories": cats,
        })
        existing_ids.add(sid)
        i += 1
    return sellers

def generate_customers(planted, target=300):
    customers = list(planted)
    existing_ids = {c["customerId"] for c in customers}
    i = 100
    while len(customers) < target:
        cid = f"cust_{i:04d}"
        if cid in existing_ids:
            i += 1
            continue
        city, state = random.choice(ALL_CITIES)
        # Bias toward SP for geographic clustering
        if random.random() < 0.35:
            city, state = random.choice(CITIES["SP"])
        order_count = random.choices([1,2,3,4,5,6,7,8], weights=[30,25,15,10,8,5,4,3])[0]
        avg_score = round(random.uniform(1.5, 5.0), 1)
        total_spent = round(random.uniform(50, 5000), 2)
        customers.append({
            "customerId": cid,
            "city": city, "state": state,
            "orderCount": order_count,
            "avgReviewScore": avg_score,
            "totalSpent": total_spent,
        })
        existing_ids.add(cid)
        i += 1
    return customers

def generate_products(planted, target=150):
    products = list(planted)
    existing_ids = {p["productId"] for p in products}
    for cat in CATEGORIES:
        prefix = cat[:4]
        for j in range(1, 12):
            pid = f"prod_{prefix}_{j:03d}"
            if pid in existing_ids:
                continue
            if len(products) >= target:
                break
            weight = random.randint(50, 15000)
            score = round(random.uniform(2.0, 5.0), 1)
            # electronics trend toward lower scores
            if cat == "electronics":
                score = round(random.uniform(1.8, 3.8), 1)
            products.append({
                "productId": pid, "category": cat,
                "weightG": weight, "avgReviewScore": score,
            })
            existing_ids.add(pid)
    return products

def make_order(order_id, customer, seller, product, pattern="normal"):
    """Generate a single order with the given pattern."""
    purchase = rand_date()
    base_delivery_days = random.randint(3, 12)
    estimated = purchase + timedelta(days=base_delivery_days)

    if pattern == "late":
        delay_days = random.randint(4, 12)
        delivered = estimated + timedelta(days=delay_days)
        score = random.choice([1, 1, 1, 2])
        comment = random.choice(REVIEW_COMMENTS_BAD)
    elif pattern == "very_late":
        delay_days = random.randint(8, 18)
        delivered = estimated + timedelta(days=delay_days)
        score = 1
        comment = random.choice(REVIEW_COMMENTS_BAD)
    elif pattern == "happy":
        early = random.randint(1, 4)
        delivered = estimated - timedelta(days=early)
        score = random.choice([4, 5, 5, 5])
        comment = random.choice(REVIEW_COMMENTS_GOOD)
    elif pattern == "bad_review":
        delivered = estimated + timedelta(days=random.randint(0, 3))
        score = 1
        comment = random.choice(REVIEW_COMMENTS_BAD)
    elif pattern == "multi_issue":
        delay_days = random.randint(5, 10)
        delivered = estimated + timedelta(days=delay_days)
        score = 1
        comment = random.choice([
            "Late AND broken. Want full refund.",
            "Both items arrived late and damaged. Seller ignored me.",
            "Late delivery, product doesn't match description, and refund was denied.",
            "Everything wrong: late, defective, no customer service.",
        ])
    elif pattern == "shipped":
        delivered = None
        score = None
        comment = None
    else:  # normal
        on_time = random.random() < 0.82
        if on_time:
            delivered = estimated - timedelta(days=random.randint(0, 2))
        else:
            delivered = estimated + timedelta(days=random.randint(1, 5))
        score = random.choices([1,2,3,4,5], weights=[8,10,15,30,37])[0]
        if score >= 4:
            comment = random.choice(REVIEW_COMMENTS_GOOD) if random.random() < 0.7 else None
        elif score <= 2:
            comment = random.choice(REVIEW_COMMENTS_BAD) if random.random() < 0.8 else None
        else:
            comment = random.choice(REVIEW_COMMENTS_MED) if random.random() < 0.5 else None

    price = round(random.uniform(25, 600), 2)
    freight = round(random.uniform(8, 55), 2)
    order_value = round(price + freight, 2)
    pay_type = random.choices(PAYMENT_TYPES, weights=[60,20,15,5])[0]
    installments = 1 if pay_type != "credit_card" else random.choices([1,2,3,4,5,6,8,10], weights=[20,15,15,15,10,10,10,5])[0]

    review = None
    if score is not None:
        review = {"score": score}
        if comment:
            review["comment"] = comment

    return {
        "orderId": order_id,
        "customerId": customer["customerId"],
        "status": "shipped" if delivered is None else "delivered",
        "purchaseTimestamp": fmt_dt(purchase),
        "deliveredTimestamp": fmt_dt(delivered),
        "estimatedDelivery": fmt_dt(estimated),
        "items": [{
            "sellerId": seller["sellerId"],
            "productId": product["productId"],
            "price": price,
            "freightValue": freight,
        }],
        "payment": {"type": pay_type, "installments": installments, "value": order_value},
        "review": review,
        "orderValue": order_value,
    }

def generate_orders(planted_orders, sellers, customers, products, target=400):
    orders = list(planted_orders)
    existing_ids = {o["orderId"] for o in orders}
    seller_map = {s["sellerId"]: s for s in sellers}
    planted_seller_ids = {s["sellerId"] for s in PLANTED_SELLERS}
    bad_sellers = [s for s in sellers if s["avgRating"] < 3.0]
    good_sellers = [s for s in sellers if s["avgRating"] >= 4.0]
    sp_customers = [c for c in customers if c["state"] == "SP"]
    all_customers = list(customers)

    def pick_product(seller):
        cat = random.choice(seller.get("categories", ["electronics"]))
        matching = [p for p in products if p["category"] == cat]
        return random.choice(matching) if matching else random.choice(products)

    counter = 1

    # ─── Late deliveries (~80) ───
    for _ in range(80):
        oid = f"ORD_LT{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_LT{counter:03d}"
        seller = random.choice(bad_sellers + [seller_map["seller_42"]]*3 + [seller_map["seller_18"]]*2)
        cust = random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "late"))
        existing_ids.add(oid)
        counter += 1

    # ─── 1-star reviews with comments (~50) ───
    for _ in range(50):
        oid = f"ORD_BR{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_BR{counter:03d}"
        seller = random.choice(bad_sellers + [seller_map["seller_42"]]*2)
        cust = random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "bad_review"))
        existing_ids.add(oid)
        counter += 1

    # ─── Multi-issue orders (~20) ───
    for _ in range(20):
        oid = f"ORD_MI{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_MI{counter:03d}"
        seller = random.choice([seller_map["seller_42"], seller_map["seller_18"], seller_map["seller_44"]] + bad_sellers)
        cust = random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "multi_issue"))
        existing_ids.add(oid)
        counter += 1

    # ─── Happy path (~100) ───
    for _ in range(100):
        oid = f"ORD_HP{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_HP{counter:03d}"
        seller = random.choice(good_sellers + [seller_map["seller_7"]]*3 + [seller_map["seller_27"]]*2)
        cust = random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "happy"))
        existing_ids.add(oid)
        counter += 1

    # ─── Geographic cluster (SP) (~80) ───
    for _ in range(80):
        oid = f"ORD_SP{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_SP{counter:03d}"
        sp_sellers = [s for s in sellers if s["state"] == "SP"]
        seller = random.choice(sp_sellers) if sp_sellers else random.choice(sellers)
        cust = random.choice(sp_customers) if sp_customers else random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "normal"))
        existing_ids.add(oid)
        counter += 1

    # ─── Normal variety (fill to target) ───
    while len(orders) < target:
        oid = f"ORD_NR{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_NR{counter:03d}"
        seller = random.choice(sellers)
        cust = random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "normal"))
        existing_ids.add(oid)
        counter += 1

    # ─── A few shipped (in-transit) orders ───
    for _ in range(8):
        oid = f"ORD_SH{counter:03d}"
        while oid in existing_ids:
            counter += 1
            oid = f"ORD_SH{counter:03d}"
        seller = random.choice(sellers)
        cust = random.choice(all_customers)
        prod = pick_product(seller)
        orders.append(make_order(oid, cust, seller, prod, "shipped"))
        existing_ids.add(oid)
        counter += 1

    return orders

def update_customer_stats(customers, orders):
    """Recompute customer stats from actual orders."""
    cust_map = {c["customerId"]: c for c in customers}
    for c in customers:
        c["_orders"] = []
    for o in orders:
        cid = o["customerId"]
        if cid in cust_map:
            cust_map[cid]["_orders"].append(o)
    for c in customers:
        o_list = c.pop("_orders", [])
        if o_list:
            c["orderCount"] = len(o_list)
            scores = [o["review"]["score"] for o in o_list if o.get("review") and o["review"].get("score")]
            c["avgReviewScore"] = round(sum(scores)/len(scores), 1) if scores else 0.0
            c["totalSpent"] = round(sum(o["orderValue"] for o in o_list), 2)

# ─── Main ──────────────────────────────────────────────────────────
def main():
    print("Generating expanded Olist dataset...")

    # Load existing planted orders
    planted_orders = []
    if PLANTED_ORDERS_FILE.exists():
        with open(PLANTED_ORDERS_FILE) as f:
            planted_orders = json.load(f)
        print(f"  Loaded {len(planted_orders)} planted orders")

    # Generate entities
    sellers = generate_sellers(PLANTED_SELLERS, target=65)
    customers = generate_customers(PLANTED_CUSTOMERS, target=300)
    products = generate_products(PLANTED_PRODUCTS, target=150)

    print(f"  Generated {len(sellers)} sellers")
    print(f"  Generated {len(customers)} customers")
    print(f"  Generated {len(products)} products")

    # Generate orders
    orders = generate_orders(planted_orders, sellers, customers, products, target=420)
    print(f"  Generated {len(orders)} orders")

    # Update customer stats from actual orders
    update_customer_stats(customers, orders)

    # Write output
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    with open(OUT_DIR / "curated-orders.json", "w") as f:
        json.dump(orders, f, indent=2, ensure_ascii=False)
    with open(OUT_DIR / "curated-sellers.json", "w") as f:
        json.dump(sellers, f, indent=2, ensure_ascii=False)
    with open(OUT_DIR / "curated-customers.json", "w") as f:
        json.dump(customers, f, indent=2, ensure_ascii=False)
    with open(OUT_DIR / "curated-products.json", "w") as f:
        json.dump(products, f, indent=2, ensure_ascii=False)

    # Summary stats
    late = sum(1 for o in orders if o.get("deliveredTimestamp") and o.get("estimatedDelivery")
               and o["deliveredTimestamp"] > o["estimatedDelivery"])
    one_star = sum(1 for o in orders if o.get("review") and o["review"].get("score") == 1)
    five_star = sum(1 for o in orders if o.get("review") and o["review"].get("score") == 5)
    sp_orders = sum(1 for o in orders
                    for item in o.get("items", [])
                    if any(s["sellerId"] == item["sellerId"] and s["state"] == "SP" for s in sellers))

    print(f"\n  Output written to {OUT_DIR}/")
    print(f"  Orders: {len(orders)} ({late} late, {one_star} 1-star, {five_star} 5-star)")
    print(f"  Sellers: {len(sellers)}")
    print(f"  Customers: {len(customers)}")
    print(f"  Products: {len(products)}")
    print(f"  SP orders: {sp_orders}")
    print(f"\n  Key entities preserved:")
    print(f"    seller_42: {next((s for s in sellers if s['sellerId']=='seller_42'), 'MISSING')!='MISSING'}")
    print(f"    seller_7:  {next((s for s in sellers if s['sellerId']=='seller_7'), 'MISSING')!='MISSING'}")
    print(f"    cust_1234: {next((c for c in customers if c['customerId']=='cust_1234'), 'MISSING')!='MISSING'}")
    print(f"    ORD_A1B2C3: {next((o for o in orders if o['orderId']=='ORD_A1B2C3'), 'MISSING')!='MISSING'}")
    print(f"    ORD_LATE01: {next((o for o in orders if o['orderId']=='ORD_LATE01'), 'MISSING')!='MISSING'}")
    print(f"    ORD_X7Y8Z9: {next((o for o in orders if o['orderId']=='ORD_X7Y8Z9'), 'MISSING')!='MISSING'}")

if __name__ == "__main__":
    main()
