"""
Google play lib
"""

from pyquery import PyQuery as pq
import time

def categoryFromPackage(package):
    base = "https://play.google.com/store/apps/details?id={}"
    url = base.format(package)
    try:
        dom = pq(url = url)
        category = dom("a[itemprop='genre']").text()
    except:
        category = "Unknown"
    print(category)
    time.sleep(.250)
    return category

print(categoryFromPackage("com.reddit.frontpage"))
